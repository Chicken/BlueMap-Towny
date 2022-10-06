package codes.antti.bluemaptowny;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.palmergames.adventure.text.Component;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class BlueMapTowny extends JavaPlugin {
    private final Map<String, MarkerSet> townMarkerSets = new ConcurrentHashMap<>();
    private Configuration config;

    @Override
    public void onEnable() {
        BlueMapAPI.onEnable((api) -> {
            reloadConfig();
            saveDefaultConfig();
            this.config = getConfig();
            initMarkerSets();
            Bukkit.getScheduler().runTaskTimer(this, this::updateMarkers, 0, this.config.getLong("update-interval") * 20);
        });
        BlueMapAPI.onDisable((api) -> {
            Bukkit.getScheduler().cancelTasks(this);
        });
    }

    private void initMarkerSets() {
        BlueMapAPI.getInstance().ifPresent((api) -> {
            townMarkerSets.clear();
            for (World world : Bukkit.getWorlds()) {
                api.getWorld(world.getName()).ifPresent((bmWorld) -> {
                    MarkerSet set = new MarkerSet("Towns");
                    townMarkerSets.put(world.getName(), set);
                    bmWorld.getMaps().forEach((map) -> {
                        map.getMarkerSets().put("towny", set);
                    });
                });
            }
        });
    }

    private Color getFillColor(Town town) {
        String opacity = String.format("%02X", (int) (this.config.getDouble("style.fill-opacity") * 255));

        if (this.config.getBoolean("dynamic-town-colors")) {
            String hex = town.getMapColorHexCode();
            if (hex != null) {
                return new Color("#" + hex + opacity);
            }
        }

        if (this.config.getBoolean("dynamic-nation-colors")) {
            String hex = town.getNationMapColorHexCode();
            if (hex != null) {
                return new Color("#" + hex + opacity);
            }
        }

        return new Color(this.config.getString("style.fill-color") + opacity);
    }

    private Color getLineColor(Town town) {
        String opacity = String.format("%02X", (int) (this.config.getDouble("style.border-opacity") * 255));

        if (this.config.getBoolean("dynamic-nation-colors")) {
            String hex = town.getNationMapColorHexCode();
            if (hex != null) {
                return new Color("#" + hex + opacity);
            }
        }

        if (this.config.getBoolean("dynamic-town-colors")) {
            String hex = town.getMapColorHexCode();
            if (hex != null) {
                return new Color("#" + hex + opacity);
            }
        }

        return new Color(this.config.getString("style.border-color") + opacity);
    }

    private String fillPlaceholders(String template, Town town) {
        String t = template;

        t = t.replace("%name%", town.getName());

        t = t.replace("%mayor%", town.hasMayor() ? town.getMayor().getName() : "");

        String[] residents = town.getResidents().stream().map(TownyObject::getName).toList().toArray(String[]::new);
        if (residents.length > 34) {
            String[] old = residents;
            residents = new String[35 + 1];
            System.arraycopy(old, 0, residents, 0, 35);
            residents[35] = "and more...";
        }
        t = t.replace("%residents%", String.join(", ", residents));

        String[] residentsDisplay = town.getResidents().stream().map((r) -> {
            Player p = Bukkit.getPlayer(r.getName());
            if (p == null) return r.getFormattedName();
            return p.displayName().toString();
        }).toList().toArray(String[]::new);
        if (residentsDisplay.length > 34) {
            String[] old = residentsDisplay;
            residentsDisplay = new String[35 + 1];
            System.arraycopy(old, 0, residentsDisplay, 0, 35);
            residentsDisplay[35] = "and more...";
        }
        t = t.replace("%residentdisplaynames%", String.join(", ", residentsDisplay));

        t = t.replace("%assistants%", String.join(", ", town.getRank("assistant").stream().map(TownyObject::getName).toList().toArray(new String[0])));

        t = t.replace("%residentcount%", "" + town.getResidents().size());

        t = t.replace("%founded%", town.getRegistered() != 0 ? TownyFormatter.registeredFormat.format(town.getRegistered()) : "None");

        t = t.replace("%board%", town.getBoard());

        t = t.replace("%trusted%", town.getTrustedResidents().isEmpty() ? "None" : town.getTrustedResidents().stream().map(TownyObject::getName).collect(Collectors.joining(", ")));

        if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive()) {
            if (town.isTaxPercentage()) t = t.replace("%tax%", town.getTaxes() + "%");
            else t = t.replace("%tax%", TownyEconomyHandler.getFormattedBalance(town.getTaxes()));
            t = t.replace("%bank%", TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()));
        }

        String nation = town.hasNation() ? Objects.requireNonNull(town.getNationOrNull()).getName() : "";
        t = t.replace("%nation%", nation);
        t = t.replace("%nationstatus%", town.hasNation() ? (town.isCapital() ? "Capital of " + nation : "Member of " + nation) : "");

        t = t.replace("%public%", town.isPublic() ? "true" : "false");

        t = t.replace("%peaceful%", town.isNeutral() ? "true" : "false");

        List<String> flags = new ArrayList<>();
        flags.add("Has Upkeep: " + town.hasUpkeep());
        flags.add("PvP: " + town.isPVP());
        flags.add("Mobs: " + town.hasMobs());
        flags.add("Explosion: " + town.isBANG());
        flags.add("Fire: " + town.isFire());
        flags.add("Nation: " + nation);
        if (TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_ENABLED)) {
            String ruinedString = "Ruined: " + town.isRuined();
            if (town.isRuined()) ruinedString += " (Time left: " + (TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)) + " hours)";
            flags.add(ruinedString);
        }
        t = t.replace("%flags%", String.join("<br />", flags));

        return t;
    }

    private void updateMarkers() {
        BlueMapAPI.getInstance().ifPresent((api) -> {
            for (World world : Bukkit.getWorlds()) {
                if (api.getWorld(world.getName()).isEmpty()) continue;
                Map<String, Marker> markers = townMarkerSets.get(world.getName()).getMarkers();
                markers.clear();
                Objects.requireNonNull(TownyAPI.getInstance().getTownyWorld(world)).getTowns().forEach((k, town) -> {
                    List<List<Vector2d>> borders = new ArrayList<>();
                    List<List<Vector2d>> areas = new ArrayList<>();
                    Set<Vector2i> chunks = town.getTownBlocks().stream().map((tb) -> new Vector2i(tb.getX(), tb.getZ())).collect(Collectors.toSet());
                    MapUtils.areaToBlockPolygon(chunks, TownySettings.getTownBlockSize(), areas, borders);
                    int layerY = this.config.getInt("style.y-level");
                    String townName = town.getName();
                    String townDetails = fillPlaceholders(this.config.getString("popup"), town);
                    int seq = 0;
                    for (List<Vector2d> area : areas) {
                        ShapeMarker chunkMarker = new ShapeMarker.Builder()
                                .label(townName)
                                .detail(townDetails)
                                .lineColor(new Color(0))
                                .fillColor(getFillColor(town))
                                .depthTestEnabled(false)
                                .shape(new Shape(area), (float) layerY)
                                .centerPosition()
                                .build();
                        markers.put("towny." + townName + ".area." + seq, chunkMarker);
                        seq += 1;
                    }
                    seq = 0;
                    for (List<Vector2d> border : borders) {
                        LineMarker borderMarker = new LineMarker.Builder()
                                .label(townName)
                                .detail(townDetails)
                                .lineColor(getLineColor(town))
                                .lineWidth(this.config.getInt("style.border-width"))
                                .depthTestEnabled(false)
                                .line(new Line(border.stream().map(v2 -> Vector3d.from(v2.getX(), layerY, v2.getY())).toList()))
                                .centerPosition()
                                .build();
                        markers.put("towny." + townName + ".border." + seq, borderMarker);
                        seq += 1;
                    }
                    Optional<Location> spawn = Optional.ofNullable(town.getSpawnOrNull());
                    if (this.config.getBoolean("style.capital-icon-enabled") && spawn.isPresent() && town.isCapital()) {
                        POIMarker iconMarker = new POIMarker.Builder()
                                .label(townName)
                                // TODO: .detail(townDetails) - not a BlueMap feature yet
                                .icon(this.config.getString("style.capital-icon"), 8, 8)
                                .position((int) spawn.get().getX(), layerY, (int) spawn.get().getZ())
                                .build();
                        markers.put("towny." + townName + ".icon", iconMarker);
                    } else if (this.config.getBoolean("style.war-icon-enabled") && spawn.isPresent() && town.hasActiveWar()){
                        POIMarker iconMarker = new POIMarker.Builder()
                                .label(townName)
                                .icon(this.config.getString("style.war-icon"), 8, 8)
                                .position((int) spawn.get().getX(), layerY, (int) spawn.get().getZ())
                                .build();
                        town.sendMessage(Component.text("test"));
                        markers.put("towny." + townName + ".icon", iconMarker);
                    } else if (this.config.getBoolean("style.home-icon-enabled") && spawn.isPresent()) {
                        POIMarker iconMarker = new POIMarker.Builder()
                                .label(townName)
                                // TODO: .detail(townDetails) - not a BlueMap feature yet
                                .icon(this.config.getString("style.home-icon"), 8, 8)
                                .position((int) spawn.get().getX(), layerY, (int) spawn.get().getZ())
                                .build();
                        markers.put("towny." + townName + ".icon", iconMarker);
                    }
                });
            }
        });
    }
}
