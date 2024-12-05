package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

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

    static void run(SpartanProtocol protocol, Runnable runnable, boolean sync) {
        if (!MultiVersion.folia) {
            if (sync && !Bukkit.isPrimaryThread()) {
                if (Register.isPluginEnabled()) {
                    Bukkit.getScheduler().runTask(Register.plugin, runnable);
                }
            } else {
                runnable.run();
            }
        } else {
            if (protocol != null) {
                if (Register.isPluginEnabled()) {
                    Location location = protocol.getLocation();
                    Bukkit.getRegionScheduler()
                            .execute(Register.plugin,
                                    location.getWorld(),
                                    SpartanLocation.getChunkPos(location.getBlockX()),
                                    SpartanLocation.getChunkPos(location.getBlockZ()),
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

    static Object schedule(SpartanProtocol protocol, Runnable runnable, long start, long repetition) {
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
                    if (protocol != null) {
                        Location location = protocol.getLocation();
                        return Bukkit.getRegionScheduler()
                                .runDelayed(Register.plugin,
                                        location.getWorld(),
                                        SpartanLocation.getChunkPos(location.getBlockX()),
                                        SpartanLocation.getChunkPos(location.getBlockZ()),
                                        consumer -> runnable.run(), start);
                    } else {
                        return Bukkit.getGlobalRegionScheduler()
                                .runDelayed(Register.plugin, consumer -> runnable.run(), start);
                    }
                } else {
                    if (protocol != null) {
                        Location location = protocol.getLocation();
                        return Bukkit.getRegionScheduler()
                                .runAtFixedRate(Register.plugin,
                                        location.getWorld(),
                                        SpartanLocation.getChunkPos(location.getBlockX()),
                                        SpartanLocation.getChunkPos(location.getBlockZ()),
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
