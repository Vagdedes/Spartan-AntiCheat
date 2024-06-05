package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;

import java.util.*;

public class PlayerDetectionSlots {

    private static final List<UUID>
            list = Collections.synchronizedList(new ArrayList<>()),
            priority = Collections.synchronizedList(new ArrayList<>());

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            int amount = CloudBase.getDetectionSlots();

            if (amount > 0) {
                Set<UUID> players = SpartanBukkit.getUUIDs();
                int playerAmount = players.size();

                if (playerAmount <= amount) {
                    synchronized (priority) {
                        synchronized (list) {
                            priority.clear();
                            list.clear();
                            list.addAll(players);
                        }
                    }
                } else {
                    List<UUID> newList = new ArrayList<>(amount);

                    // Add prioritised players first
                    if (!priority.isEmpty()) {
                        synchronized (priority) {
                            Iterator<UUID> iterator = priority.iterator();

                            while (iterator.hasNext() && newList.size() < amount) {
                                newList.add(iterator.next());
                                iterator.remove();
                            }
                        }
                    }

                    // Add non-prioritised players that are not being checked
                    synchronized (list) {
                        for (UUID uuid : players) {
                            if (!list.contains(uuid)) {
                                if (newList.size() < amount) {
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
                            if (newList.size() < amount) {
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
            }
        }, 1L, 1L);
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
            } else if (list.size() < optionAmount) {
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
        int amount = CloudBase.getDetectionSlots();
        return amount > 0 && add(player.uuid, amount);
    }

    public static boolean isChecked(UUID uuid) {
        int amount = CloudBase.getDetectionSlots();

        if (amount <= 0) {
            return true;
        } else {
            return add(uuid, amount);
        }
    }

}
