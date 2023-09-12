package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MaximumCheckedPlayers {

    private static final CopyOnWriteArrayList<UUID> list = new CopyOnWriteArrayList<>();
    private static final List<UUID> priority = new ArrayList<>();
    private static final int minimumPlayers = 20, refreshTicks = 20;
    public static final String option = "Performance.maximum_checked_players_at_once";
    private static int ticks = 0;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                int optionAmount = Settings.getInteger(option);

                if (optionAmount > 0) {
                    if (ticks == 0) {
                        ticks = refreshTicks;
                        optionAmount = Math.max(optionAmount, minimumPlayers);
                        Set<UUID> players = SpartanBukkit.getUUIDs();
                        int playerAmount = players.size();

                        if (playerAmount <= optionAmount) {
                            priority.clear();
                            list.clear();
                            list.addAll(players);
                        } else {
                            List<UUID> newList = new ArrayList<>(optionAmount);

                            // Add prioritised players first
                            if (priority.size() > 0) {
                                Iterator<UUID> iterator = priority.iterator();

                                while (iterator.hasNext() && newList.size() < optionAmount) {
                                    newList.add(iterator.next());
                                    iterator.remove();
                                }
                            }

                            // Add non-prioritised players that are not being checked
                            for (UUID uuid : players) {
                                if (!list.contains(uuid)) {
                                    if (newList.size() < optionAmount) {
                                        newList.add(uuid);
                                    } else if (!priority.contains(uuid)) {
                                        priority.add(uuid);
                                    }
                                }
                            }

                            // Add non-prioritised players that are being checked
                            for (UUID uuid : list) {
                                if (newList.size() < optionAmount) {
                                    newList.add(uuid);
                                } else if (!priority.contains(uuid)) {
                                    priority.add(uuid);
                                }
                            }

                            // Set the new list
                            list.clear();
                            list.addAll(newList);
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

    static boolean isActive() {
        int optionAmount = Settings.getInteger(option);
        return optionAmount > 0
                && SpartanBukkit.getPlayerCount() > Math.max(optionAmount, minimumPlayers);
    }

    public static void remove(SpartanPlayer player) {
        UUID uuid = player.getUniqueId();
        priority.remove(uuid);
        list.remove(uuid);
    }

    private static boolean add(UUID uuid, int optionAmount) {
        if (list.size() < Math.max(optionAmount, minimumPlayers) && list.addIfAbsent(uuid)) {
            priority.remove(uuid);
            return true;
        }
        return false;
    }

    public static boolean add(SpartanPlayer player) {
        int optionAmount = Settings.getInteger(option);
        return optionAmount > 0 && add(player.getUniqueId(), optionAmount);
    }

    public static boolean isChecked(UUID uuid) {
        int optionAmount = Settings.getInteger(option);
        return optionAmount <= 0
                || list.contains(uuid)
                || add(uuid, optionAmount)
                || TestServer.isIdentified();
    }
}
