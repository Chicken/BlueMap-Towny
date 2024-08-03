package codes.antti.bluemaptowny.TownyCommands;

import codes.antti.bluemaptowny.BlueMapTowny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SetTownMarker implements CommandExecutor, TabExecutor {

    Plugin plugin = BlueMapTowny.plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            TownyMessaging.sendErrorMsg(sender, "Must be run by a Player!");
            return true;
        }
        Resident resident = TownyUniverse.getInstance().getResident(((Player) sender).getUniqueId());

        if (!resident.hasTown()) {
            TownyMessaging.sendErrorMsg(sender, "You don't have a town!");
            return true;
        }

        if (!resident.isMayor() || !((Player) sender).getPlayer().hasPermission("towny.command.town.set.townmarker")) {
            TownyMessaging.sendErrorMsg(sender, "You don't have permission for this or aren't the Mayor!");
            return true;
        }

        if(plugin.getConfig().getString("home-icon-style").equalsIgnoreCase("preset")){
            if(ifFileExists(String.join(" ", args)) == false){
                TownyMessaging.sendErrorMsg(resident, "This file isn't a valid file!");
                return true;
            }
        }
        TownyMessaging.sendPrefixedTownMessage(resident.getTownOrNull(), "Town Marker has been set to: " + String.join(" ", args));
        resident.getTownOrNull().addMetaData(new StringDataField("mapMarker", String.join(" ", args)));


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (plugin.getConfig().getInt("use-links-as-image-source")) {
            case 1 -> {
                File file = new File(BlueMapAPI.getInstance().get().getWebApp().getWebRoot().toFile(), "/assets/TownMarkers");
                String[] listOfFiles = file.list();
                if (args.length == 1) {
                    return Arrays.stream(listOfFiles).toList();
                }
                return Collections.emptyList();
            }
            case 2 -> {
                return Collections.singletonList("<link>");
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public boolean ifFileExists(String string){
        File file = new File(BlueMapAPI.getInstance().get().getWebApp().getWebRoot().toFile(), "/assets/townmarkers/" + string);
        if(file.isFile()) {
            return true;
        }
        return false;
    }
}
