package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TestServer;

import java.util.*;

public class MaximumCheckedPlayers {

    private static final List<UUID>
            list = Collections.synchronizedList(new ArrayList<>()),
            priority = Collections.synchronizedList(new ArrayList<>());
    private static final int minimumPlayers = 20, refreshTicks = 20;
    public static final String option = "Performance.maximum_checked_players_at_once";
    private static int ticks = 0;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                int optionAmount = Config.settings.getInteger(option);

                if (optionAmount > 0) {
                    if (ticks == 0) {
                        ticks = refreshTicks;
                        optionAmount = Math.max(optionAmount, minimumPlayers);
                        Set<UUID> players = SpartanBukkit.getUUIDs();
                        int playerAmount = players.size();

                        if (playerAmount <= optionAmount) {
                            synchronized (priority) {
                                synchronized (list) {
                                    priority.clear();
                                    list.clear();
                                    list.addAll(players);
                                }
                            }
                        } else {
                            List<UUID> newList = new ArrayList<>(optionAmount);

                            // Add prioritised players first
                            if (!priority.isEmpty()) {
                                synchronized (priority) {
                                    Iterator<UUID> iterator = priority.iterator();

                                    while (iterator.hasNext() && newList.size() < optionAmount) {
                                        newList.add(iterator.next());
                                        iterator.remove();
                                    }
                                }
                            }

                            // Add non-prioritised players that are not being checked
                            synchronized (list) {
                                for (UUID uuid : players) {
                                    if (!list.contains(uuid)) {
                                        if (newList.size() < optionAmount) {
                                            newList.add(uuid);
                                        } else {
                                            synchronized (priority) {
                                                if (!priority.contains(uuid)) {
                                                    priority.add(uuid);
                                                }
                                            }
                                        }
                                    }
                                }

                                // Add non-prioritised players that are being checked
                                for (UUID uuid : list) {
                                    if (newList.size() < optionAmount) {
                                        newList.add(uuid);
                                    } else {
                                        synchronized (priority) {
                                            if (!priority.contains(uuid)) {
                                                priority.add(uuid);
                                            }
                                        }
                                    }
                                }

                                // Set the new list
                                list.clear();
                                list.addAll(newList);
                            }
                        }
                    } else {
                        ticks -= 1;
                    }
                } else {
                    ticks = refreshTicks;
                }
            }, 1L, 1L);
        }
    }

    public static void remove(SpartanPlayer player) {
        UUID uuid = player.uuid;

        synchronized (priority) {
            synchronized (list) {
                priority.remove(uuid);
                list.remove(uuid);
            }
        }
    }

    private static boolean add(UUID uuid, int optionAmount) {
        synchronized (list) {
            if (list.contains(uuid)) {
                return true;
            } else if (list.size() < Math.max(optionAmount, minimumPlayers)) {
                list.add(uuid);

                synchronized (priority) {
                    priority.remove(uuid);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean add(SpartanPlayer player) {
        int optionAmount = Config.settings.getInteger(option);
        return optionAmount > 0 && add(player.uuid, optionAmount);
    }

    public static boolean isChecked(UUID uuid) {
        if (TestServer.isIdentified()) {
            return true;
        } else {
            int optionAmount = Config.settings.getInteger(option);

            if (optionAmount <= 0) {
                return true;
            } else {
                return add(uuid, optionAmount);
            }
        }
    }
}
