package me.vagdedes.spartan.features.moderation;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Spectate {

    private static class Properties {

        private final SpartanPlayer target;
        private final SpartanLocation initialLocation;

        public Properties(SpartanPlayer target, SpartanLocation initialLocation) {
            this.target = target;
            this.initialLocation = initialLocation;
        }
    }

    private static final Map<SpartanPlayer, Properties> memory = new LinkedHashMap<>(Config.getMaxPlayers());

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                Set<Map.Entry<SpartanPlayer, Properties>> entries = memory.entrySet();
                int size = entries.size();

                if (size > 0) {
                    Map<SpartanPlayer, Boolean> toRemove = new LinkedHashMap<>(size);

                    for (Map.Entry<SpartanPlayer, Properties> entry : entries) {
                        SpartanPlayer p = entry.getKey();

                        SpartanBukkit.runTask(p, () -> {
                            if (!Permissions.has(p, Enums.Permission.INFO)
                                    && !Permissions.has(p, Enums.Permission.MANAGE)) {
                                toRemove.put(p, true);
                            } else {
                                SpartanPlayer t = entry.getValue().target;

                                if (t == null) {
                                    toRemove.put(p, true);
                                } else {
                                    SpartanLocation location = p.getLocation();
                                    SpartanLocation targetLocation = t.getLocation();

                                    if (!location.getWorld().equals(targetLocation.getWorld())) {
                                        toRemove.put(p, false);
                                    } else if (AlgebraUtils.getHorizontalDistance(location, targetLocation) >= (MoveUtils.chunk * 3)) {
                                        toRemove.put(p, true);
                                    } else {
                                        appearance(p, false);
                                    }
                                }
                            }
                        });
                    }
                    for (Map.Entry<SpartanPlayer, Boolean> entry : toRemove.entrySet()) {
                        remove(entry.getKey(), entry.getValue());
                    }
                }
            }, 1L, 4L);
        }
    }

    public static void clear() {
        for (SpartanPlayer p : memory.keySet()) {
            appearance(p, true);
        }
        memory.clear();
    }

    public static boolean isTarget(SpartanPlayer p, SpartanPlayer t) {
        SpartanPlayer target = getTarget(p);
        return target != null && target.equals(t);
    }

    public static boolean isUsing(SpartanPlayer p) {
        return getTarget(p) != null;
    }

    public static SpartanPlayer getTarget(SpartanPlayer p) {
        Properties properties = memory.get(p);
        return properties == null ? null : properties.target;
    }

    public static void remove(SpartanPlayer p, boolean teleport) {
        Properties properties = memory.remove(p);

        if (properties != null) {
            if (teleport) {
                p.teleport(properties.initialLocation);
            }
            appearance(p, true);

            String message = ConfigUtils.replaceWithSyntax(p, Messages.get("spectating_ended"), null);
            p.sendMessage(message);
        }
    }

    public static void run(SpartanPlayer p, SpartanPlayer t) {
        if (!p.equals(t)) {
            Properties properties = memory.get(p);
            memory.put(p, new Properties(t, properties != null ? properties.initialLocation : p.getLocation()));
            p.teleport(t.getLocation());
            appearance(p, false);

            String message = ConfigUtils.replaceWithSyntax(t, Messages.get("spectating_player"), null);
            p.sendInventoryCloseMessage(message); // Close inventory
        } else {
            p.sendInventoryCloseMessage(null);
        }
    }

    private static void appearance(SpartanPlayer p, boolean show) {
        if (show) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        }
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer so : players) {
                if (!so.equals(p)) {
                    if (show) {
                        if (!so.canSee(p)) {
                            so.showPlayer(p);
                        }
                    } else if (so.canSee(p)) {
                        so.hidePlayer(p);
                    }
                }
            }
        }
    }

}
