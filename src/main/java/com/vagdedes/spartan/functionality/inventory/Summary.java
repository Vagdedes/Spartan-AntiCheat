package com.vagdedes.spartan.functionality.inventory;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.StatisticalProgress;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Summary {

    public static List<String> get() {
        int arraySize = 20;
        List<String> array = new ArrayList<>(arraySize),
                statisticsArray = new ArrayList<>(arraySize);
        StatisticalProgress object = ResearchEngine.getProgress();
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        double players = playerProfiles.size(); // purposely double to help with the divisions

        if (players > 0) {
            Collection<SpartanPlayer> staffOnline = object.getStaffOnline();
            int hackers = ResearchEngine.getHackers().size(),
                    suspectedPlayers = ResearchEngine.getSuspectedPlayers().size(),
                    legitimates = ResearchEngine.getLegitimatePlayers().size(),
                    staffOnlineAmount = staffOnline.size();

            // Separator

            if (hackers > 0) {
                statisticsArray.add("§c" + AlgebraUtils.integerRound((hackers / players) * 100.0) + "§r§c% §7of players are §chackers");
            }
            if (suspectedPlayers > 0) {
                statisticsArray.add("§c" + AlgebraUtils.integerRound((suspectedPlayers / players) * 100.0) + "§r§c% §7of players are §csuspected");
            }
            if (legitimates > 0) {
                statisticsArray.add("§c" + AlgebraUtils.integerRound((legitimates / players) * 100.0) + "§r§c% §7of players are §clegitimate");
            }
            if (object.kicks > 0
                    || object.warnings > 0
                    || object.punishments > 0) {
                statisticsArray.add("§c" + object.kicks + " §r§c" + (object.kicks == 1 ? "kick" : "kicks")
                        + "§7, §c" + object.warnings + " §r§c" + (object.warnings == 1 ? "warning" : "warnings")
                        + " §7& §c" + object.punishments + " §r§c" + (object.punishments == 1 ? "punishment" : "punishments")
                        + " §7executed");
            }
            if (object.mines > 0) {
                statisticsArray.add("§c" + object.mines + " ore " + (object.mines == 1 ? "block" : "§r§cblocks") + " have been §cmined");
            }
            if (staffOnlineAmount > 0) {
                int counter = 10;

                if (!statisticsArray.isEmpty()) {
                    statisticsArray.add("");
                }
                statisticsArray.add("§c" + staffOnlineAmount + " §7staff " + (staffOnlineAmount == 1 ? "player is" : "players are") + " §conline§8:");

                if (staffOnlineAmount > counter) {
                    counter = 0;

                    for (SpartanPlayer player : staffOnline) {
                        statisticsArray.add("§c" + player.name);
                        counter++;

                        if (counter == 10) {
                            break;
                        }
                    }
                } else {
                    for (SpartanPlayer player : staffOnline) {
                        statisticsArray.add("§c" + player.name);
                    }
                }
            }
        }

        // Separator
        InventoryUtils.prepareDescription(array, "Important Information");

        // Separator
        if (players > 0.0) {
            array.add("§7Data from §4" + ((int) players) + " stored " + (players == 1 ? "player" : "players") + "§8:");
        } else {
            array.add("§7Data from §4stored players§8:");
        }

        if (!statisticsArray.isEmpty()) {
            array.addAll(statisticsArray);
        } else {
            array.add("§7" + PlayerStateLists.noDataAvailable);
        }
        return array;
    }
}
