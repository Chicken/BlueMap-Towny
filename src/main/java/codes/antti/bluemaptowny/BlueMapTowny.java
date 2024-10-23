package codes.antti.bluemaptowny;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.flowpowered.math.vector.Vector2d;
import com.technicjelle.BMUtils.Cheese;
import de.bluecolored.bluemap.api.markers.*;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.flowpowered.math.vector.Vector2i;
import com.gmail.goosius.siegewar.SiegeWarAPI;
import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.technicjelle.UpdateChecker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

public final class BlueMapTowny extends JavaPlugin {
    private final Map<UUID, MarkerSet> townMarkerSets = new ConcurrentHashMap<>();
    private Configuration config;

    @Override
    public void onEnable() {
        try {
            UpdateChecker updateChecker = new UpdateChecker("Chicken", "BlueMap-Towny", getDescription().getVersion());
            updateChecker.check();
            updateChecker.logUpdateMessage(getLogger());
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean isFolia = isFolia();
        BlueMapAPI.onEnable((api) -> {
            reloadConfig();
            saveDefaultConfig();
            this.config = getConfig();
            initMarkerSets();
            if (isFolia) {
                Bukkit.getServer().getAsyncScheduler().runAtFixedRate(this, task -> this.updateMarkers(), 1L, this.config.getLong("update-interval"), TimeUnit.SECONDS);
            } else {
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateMarkers, 0L, this.config.getLong("update-interval") * 20);
            }
        });
        BlueMapAPI.onDisable((api) -> {
            if (isFolia) {
                Bukkit.getServer().getGlobalRegionScheduler().cancelTasks(this);
            } else {
                Bukkit.getScheduler().cancelTasks(this);
            }
        });
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void initMarkerSets() {
        BlueMapAPI.getInstance().ifPresent((api) -> {
            townMarkerSets.clear();
            for (World world : Bukkit.getWorlds()) {
                api.getWorld(world).ifPresent((bmWorld) -> {
                    MarkerSet set = new MarkerSet("Towns");
                    townMarkerSets.put(world.getUID(), set);
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
            if (hex != null && !hex.equals("")) {
                return new Color("#" + hex + opacity);
            }
        }

        if (this.config.getBoolean("dynamic-nation-colors")) {
            String hex = town.getNationMapColorHexCode();
            if (hex != null && !hex.equals("")) {
                return new Color("#" + hex + opacity);
            }
        }

        return new Color(this.config.getString("style.fill-color") + opacity);
    }

    private Color getLineColor(Town town) {
        String opacity = String.format("%02X", (int) (this.config.getDouble("style.border-opacity") * 255));

        if (this.config.getBoolean("dynamic-nation-colors")) {
            String hex = town.getNationMapColorHexCode();
            if (hex != null && !hex.equals("")) {
                return new Color("#" + hex + opacity);
            }
        }

        if (this.config.getBoolean("dynamic-town-colors")) {
            String hex = town.getMapColorHexCode();
            if (hex != null && !hex.equals("")) {
                return new Color("#" + hex + opacity);
            }
        }

        return new Color(this.config.getString("style.border-color") + opacity);
    }

    private String fillPlaceholders(String template, Town town) {
        String t = template;

        t = t.replace("%name%", town.getName().replace("_", " "));

        t = t.replace("%mayor%", town.hasMayor() ? town.getMayor().getName() : "");

        String[] residents = town.getResidents().stream().map(TownyObject::getName).toArray(String[]::new);
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
            return p.getDisplayName();
        }).toArray(String[]::new);
        if (residentsDisplay.length > 34) {
            String[] old = residentsDisplay;
            residentsDisplay = new String[35 + 1];
            System.arraycopy(old, 0, residentsDisplay, 0, 35);
            residentsDisplay[35] = "and more...";
        }
        t = t.replace("%residentdisplaynames%", String.join(", ", residentsDisplay));

        t = t.replace("%assistants%", String.join(", ", town.getRank("assistant").stream().map(TownyObject::getName).toArray(String[]::new)));

        t = t.replace("%residentcount%", "" + town.getResidents().size());

        t = t.replace("%founded%", town.getRegistered() != 0 ? TownyFormatter.registeredFormat.format(town.getRegistered()) : "None");

        t = t.replace("%board%", town.getBoard());

        t = t.replace("%trusted%", town.getTrustedResidents().isEmpty() ? "None" : town.getTrustedResidents().stream().map(TownyObject::getName).collect(Collectors.joining(", ")));

        if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive()) {
            if (town.isTaxPercentage()) t = t.replace("%tax%", town.getTaxes() + "%");
            else t = t.replace("%tax%", TownyEconomyHandler.getFormattedBalance(town.getTaxes()));
            t = t.replace("%bank%", TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()));
        }

        String nation = town.hasNation() ? Objects.requireNonNull(town.getNationOrNull()).getName().replace("_", " ") : "";
        t = t.replace("%nation%", nation);
        t = t.replace("%nationstatus%", town.hasNation() ? (town.isCapital() ? "Capital of " + nation : "Member of " + nation) : "");

        t = t.replace("%public%", town.isPublic() ? "true" : "false");

        t = t.replace("%peaceful%", town.isNeutral() ? "true" : "false");

        t = t.replace("%war%", town.hasActiveWar() ? "true" : "false");

        List<String> flags = new ArrayList<>();
        flags.add("Has Upkeep: " + town.hasUpkeep());
        flags.add("PvP: " + town.isPVP());
        flags.add("Mobs: " + town.hasMobs());
        flags.add("Explosion: " + town.isExplosion());
        flags.add("Fire: " + town.isFire());
        flags.add("Nation: " + nation);
        if (TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_ENABLED)) {
            String ruinedString = "Ruined: " + town.isRuined();
            if (town.isRuined()) ruinedString += " (Time left: " + (TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)) + " hours)";
            flags.add(ruinedString);
        }
        t = t.replace("%flags%", String.join("<br />", flags));

        if (town.hasMeta("townycultures_culture")) {
            t = t.replace("%town_culture%", Objects.requireNonNull(town.getMetadata("townycultures_culture")).getValue().toString());
        } else {
            t = t.replace("%town_culture%", "");
        }

        if (town.hasMeta("townyresources_dailyproduction")) {
            t = t.replace("%town_resources%", Objects.requireNonNull(town.getMetadata("townyresources_dailyproduction")).getValue().toString());
        } else {
            t = t.replace("%town_resources%", "");
        }

        if(getServer().getPluginManager().isPluginEnabled("SiegeWar")) {
            if (SiegeWarAPI.hasSiege(town)) {
                Siege siege = SiegeWarAPI.getSiege(town).get();
                t = t.replace("%attacker%", siege.getAttackerNameForDisplay());

                t = t.replace("%defender%", siege.getDefenderNameForDisplay());

                t = t.replace("%siege_type%", siege.getSiegeType().getName());

                t = t.replace("%sessions_completed%", String.valueOf(siege.getNumBattleSessionsCompleted()));

                t = t.replace("%sessions_total%", String.valueOf(SiegeWarSettings.getSiegeDurationBattleSessions()));

                if (TownyEconomyHandler.isActive()) {
                    t = t.replace("%war_chest%", TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));
                }

                t = t.replace("%banner_control%", WordUtils.capitalizeFully(siege.getBannerControllingSide().name())
                        + (siege.getBannerControllingSide() == SiegeSide.NOBODY ? "" :  " (" + siege.getBannerControllingResidents().size() + ")"));

                t = t.replace("%siege_status%", siege.getStatus().getName());

                t = t.replace("%siege_balance%", siege.getSiegeBalance().toString());

                t = t.replace("%battle_points_attacker%", siege.getFormattedAttackerBattlePoints());

                t = t.replace("%battle_points_defender%", siege.getFormattedDefenderBattlePoints());

                t = t.replace("%battle_time_left%", siege.getFormattedBattleTimeRemaining());

            }
        }

        return t;
    }

    private void updateMarkers() {
        BlueMapAPI.getInstance().ifPresent((api) -> {
            for (World world : Bukkit.getWorlds()) {
                if (api.getWorld(world.getName()).isEmpty()) continue;
                MarkerSet set = townMarkerSets.get(world.getUID());
                if (set == null) continue;
                Map<String, Marker> markers = set.getMarkers();
                markers.clear();
                TownyWorld townyworld = TownyAPI.getInstance().getTownyWorld(world);
                if (townyworld == null) continue;
                TownyAPI.getInstance().getTowns().forEach((town) -> {
                    Vector2i[] chunks = town.getTownBlocks().stream().filter((tb) -> tb.getWorld().equals(townyworld)).map((tb) -> new Vector2i(tb.getX(), tb.getZ())).toArray(Vector2i[]::new);
                    int townSize = TownySettings.getTownBlockSize();
                    Vector2d cellSize = new Vector2d(townSize, townSize);
                    Collection<Cheese> cheeses = Cheese.createPlatterFromCells(cellSize, chunks);
                    double layerY = this.config.getDouble("style.y-level");
                    String townName = town.getName();
                    String townDetails = fillPlaceholders(this.config.getString("popup"), town);
                    String siegeDetails = fillPlaceholders(this.config.getString("popup-siege"), town);
                    int seq = 0;
                    for (Cheese cheese : cheeses) {
                        ShapeMarker.Builder chunkMarkerBuilder = new ShapeMarker.Builder()
                                .label(townName)
                                .detail(townDetails)
                                .lineColor(getLineColor(town))
                                .lineWidth(this.config.getInt("style.border-width"))
                                .fillColor(getFillColor(town))
                                .depthTestEnabled(false)
                                .shape(cheese.getShape(), (float) layerY);
                        if (this.config.getBoolean("lie-about-holes", false) == false) chunkMarkerBuilder.holes(cheese.getHoles().toArray(Shape[]::new));
                        ShapeMarker chunkMarker = chunkMarkerBuilder
                                .centerPosition()
                                .build();
                        markers.put("towny." + townName + ".area." + seq, chunkMarker);
                        seq += 1;
                    }
                    Optional<Location> spawn = Optional.ofNullable(town.getSpawnOrNull());
                    if (spawn.isPresent() && spawn.get().getWorld().equals(world)) {
                        if (this.config.getBoolean("style.ruined-icon-enabled") && town.isRuined()) {
                            POIMarker iconMarker = new POIMarker.Builder()
                                    .label(townName)
                                    .detail(townDetails)
                                    .icon(this.config.getString("style.ruined-icon"), 8, 8)
                                    .styleClasses("towny-icon")
                                    .position(spawn.get().getX(), layerY, spawn.get().getZ())
                                    .build();
                            markers.put("towny." + townName + ".icon", iconMarker);
                        } else if (this.config.getBoolean("style.war-icon-enabled") && town.hasActiveWar()) {
                            POIMarker iconMarker = new POIMarker.Builder()
                                    .label(townName)
                                    .detail(townDetails)
                                    .icon(this.config.getString("style.war-icon"), 8, 8)
                                    .styleClasses("towny-icon")
                                    .position(spawn.get().getX(), layerY, spawn.get().getZ())
                                    .build();
                            markers.put("towny." + townName + ".icon", iconMarker);
                        } else if (this.config.getBoolean("style.capital-icon-enabled") && town.isCapital()) {
                            POIMarker iconMarker = new POIMarker.Builder()
                                    .label(townName)
                                    .detail(townDetails)
                                    .icon(this.config.getString("style.capital-icon"), 8, 8)
                                    .styleClasses("towny-icon")
                                    .position(spawn.get().getX(), layerY, spawn.get().getZ())
                                    .build();

                            markers.put("towny." + townName + ".icon", iconMarker);
                        } else if (this.config.getBoolean("style.home-icon-enabled")) {
                            POIMarker iconMarker = new POIMarker.Builder()
                                    .label(townName)
                                    .detail(townDetails)
                                    .icon(this.config.getString("style.home-icon"), 8, 8)
                                    .styleClasses("towny-icon")
                                    .position(spawn.get().getX(), layerY, spawn.get().getZ())
                                    .build();
                            markers.put("towny." + townName + ".icon", iconMarker);
                        }
                    }

                    if (getServer().getPluginManager().isPluginEnabled("SiegeWar") && this.config.getBoolean("style.war-icon-enabled") && SiegeWarAPI.hasActiveSiege(town)) {
                        Location flagLoc = SiegeWarAPI.getSiege(town).get().getFlagLocation();
                        POIMarker iconMarker = new POIMarker.Builder()
                                .label(townName)
                                .detail(siegeDetails)
                                .icon(this.config.getString("style.war-icon"), 8, 8)
                                .styleClasses("towny-icon")
                                .position(flagLoc.getX(), layerY, flagLoc.getZ())
                                .build();
                        markers.put("towny." + townName + ".siege", iconMarker);
                    }
                });
            }
        });
    }
}
