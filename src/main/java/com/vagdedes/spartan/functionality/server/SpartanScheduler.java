package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

class SpartanScheduler {

    static void transfer(Runnable runnable) {
        if (Register.isPluginEnabled()) {
            if (!MultiVersion.folia) {
                Bukkit.getScheduler().runTask(Register.plugin, runnable);
            } else {
                Bukkit.getGlobalRegionScheduler().run(Register.plugin, consumer -> runnable.run());
            }
        }
    }

    static void run(Player player, Runnable runnable, boolean sync) {
        if (!MultiVersion.folia) {
            if (sync && !Bukkit.isPrimaryThread()) {
                if (Register.isPluginEnabled()) {
                    Bukkit.getScheduler().runTask(Register.plugin, runnable);
                }
            } else {
                runnable.run();
            }
        } else {
            if (player != null) {
                if (Register.isPluginEnabled()
                        && !ProtocolLib.isTemporary(player)) {
                    Location location = player.getLocation();
                    Bukkit.getRegionScheduler()
                            .execute(Register.plugin,
                                    player.getWorld(),
                                    SpartanLocation.getChunkPos(location.getBlockX()),
                                    SpartanLocation.getChunkPos(location.getBlockZ()),
                                    runnable);
                }
            } else {
                runnable.run();
            }
        }
    }

    static void run(SpartanPlayer player, Runnable runnable, boolean sync) {
        if (!MultiVersion.folia) {
            if (sync && !Bukkit.isPrimaryThread()) {
                if (Register.isPluginEnabled()) {
                    Bukkit.getScheduler().runTask(Register.plugin, runnable);
                }
            } else {
                runnable.run();
            }
        } else {
            if (player != null) {
                if (Register.isPluginEnabled()) {
                    SpartanLocation location = player.movement.getLocation();
                    Bukkit.getRegionScheduler()
                            .execute(Register.plugin,
                                    player.getWorld(), location.getChunkX(), location.getChunkZ(),
                                    runnable);
                }
            } else {
                runnable.run();
            }
        }
    }

    static void run(World world, int x, int z, Runnable runnable, boolean sync) {
        if (!MultiVersion.folia) {
            if (sync && !Bukkit.isPrimaryThread()) {
                if (Register.isPluginEnabled()) {
                    Bukkit.getScheduler().runTask(Register.plugin, runnable);
                }
            } else {
                runnable.run();
            }
        } else if (Register.isPluginEnabled()) {
            Bukkit.getRegionScheduler()
                    .execute(Register.plugin,
                            world, x, z,
                            runnable);
        }
    }

    static Object schedule(SpartanPlayer player, Runnable runnable, long start, long repetition) {
        if (Register.isPluginEnabled()) {
            if (!MultiVersion.folia) {
                if (repetition == -1L) {
                    return Bukkit.getScheduler()
                            .scheduleSyncDelayedTask(Register.plugin, runnable, start);
                } else {
                    return Bukkit.getScheduler()
                            .scheduleSyncRepeatingTask(Register.plugin, runnable, start, repetition);
                }
            } else {
                if (repetition == -1L) {
                    if (player != null) {
                        SpartanLocation location = player.movement.getLocation();
                        return Bukkit.getRegionScheduler()
                                .runDelayed(Register.plugin,
                                        player.getWorld(), location.getChunkX(), location.getChunkZ(),
                                        consumer -> runnable.run(), start);
                    } else {
                        return Bukkit.getGlobalRegionScheduler()
                                .runDelayed(Register.plugin, consumer -> runnable.run(), start);
                    }
                } else {
                    if (player != null) {
                        SpartanLocation location = player.movement.getLocation();
                        return Bukkit.getRegionScheduler()
                                .runAtFixedRate(Register.plugin,
                                        player.getWorld(), location.getChunkX(), location.getChunkZ(),
                                        consumer -> runnable.run(), start, repetition);
                    } else {
                        return Bukkit.getGlobalRegionScheduler()
                                .runAtFixedRate(Register.plugin, consumer -> runnable.run(), start, repetition);
                    }
                }
            }
        } else {
            return null;
        }
    }

    static void cancel(Object task) {
        if (!MultiVersion.folia) {
            if (task instanceof Integer) {
                Bukkit.getScheduler().cancelTask((int) task);
            }
        } else if (task instanceof ScheduledTask) {
            ((ScheduledTask) task).cancel();
        }
    }
}
