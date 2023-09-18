package me.vagdedes.spartan.gui.helpers;

import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.features.StatisticalProgress;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class AntiCheatUpdates {

    public static final String name = "AntiCheat Updates",
            communicationDetails = "§5Official Discord§8: §d§n" + DiscordMemberCount.discordURL + "§r §8§l| §r§2Business Email§8: §acontact@vagdedes.com";

    public static List<String> getInformation(boolean showStatistics) {
        int arraySize = 20;
        double players;
        List<String> array = new ArrayList<>(arraySize),
                statisticsArray = showStatistics ? new ArrayList<>(arraySize) : null;
        boolean caching = ResearchEngine.isCaching();

        if (!showStatistics || caching) {
            players = 0.0;
        } else {
            StatisticalProgress object = ResearchEngine.getProgress();
            int amount = object.getLogs();

            if (amount > 0) {
                List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
                players = playerProfiles.size(); // purposely double to help with the divisions

                if (players > 0) {
                    SpartanPlayer[] staffOnline = object.getStaffOnline();
                    int hackers = ResearchEngine.getHackers().size(),
                            suspectedPlayers = ResearchEngine.getSuspectedPlayers().size(),
                            legitimates = ResearchEngine.getLegitimatePlayers().size(),
                            activeReports = ResearchEngine.getReports(null, 0, true).size(),
                            allReports = ResearchEngine.getReports(null, 0, false).size(),
                            bans = object.getBans(), kicks = object.getKicks(), warnings = object.getWarnings(),
                            mines = object.getMines(),
                            staffOffline = object.getStaffOffline(), staffOnlineAmount = staffOnline.length;

                    // Separator

                    if (hackers > 0) {
                        statisticsArray.add("§c" + Math.max(AlgebraUtils.cut((hackers / players) * 100.0, 2), 0.01) + "§r§c% §7of players are §chackers");
                    }
                    if (suspectedPlayers > 0) {
                        statisticsArray.add("§c" + Math.max(AlgebraUtils.cut((suspectedPlayers / players) * 100.0, 2), 0.01) + "§r§c% §7of players are §csuspected");
                    }
                    if (legitimates > 0) {
                        statisticsArray.add("§c" + Math.max(AlgebraUtils.cut((legitimates / players) * 100.0, 2), 0.01) + "§r§c% §7of players are §clegitimate");
                    }
                    if (allReports > 0) {
                        statisticsArray.add("§c" + Math.max(AlgebraUtils.cut(((allReports - activeReports) / ((double) allReports)) * 100.0, 2), 0.01) + "§r§c% §7of reports have been §chandled");
                    }
                    if (bans > 0 || kicks > 0 || warnings > 0) {
                        statisticsArray.add("§c" + bans + " " + (bans == 1 ? "ban" : "§r§cbans")
                                + "§7, §c" + kicks + " " + (kicks == 1 ? "kick" : "§r§ckicks")
                                + " §7& §c" + warnings + " " + (warnings == 1 ? "warning" : "§r§cwarnings")
                                + " §7executed");
                    }
                    if (mines > 0) {
                        statisticsArray.add("§c" + mines + " ore " + (mines == 1 ? "block" : "§r§cblocks") + " have been §cmined");
                    }
                    if (staffOffline > 0) {
                        statisticsArray.add("§c" + staffOffline + " §7staff " + (staffOffline == 1 ? "player is" : "players are") + " §coffline");
                    }
                    if (staffOnlineAmount > 0) {
                        int counter = 10;

                        if (statisticsArray.size() > 0) {
                            statisticsArray.add("");
                        }
                        statisticsArray.add("§c" + staffOnlineAmount + " §7staff " + (staffOnlineAmount == 1 ? "player is" : "players are") + " §conline§8:");

                        if (staffOnlineAmount > counter) {
                            counter = 0;

                            for (SpartanPlayer player : staffOnline) {
                                statisticsArray.add("§c" + player.getName());
                                counter++;

                                if (counter == 10) {
                                    break;
                                }
                            }
                        } else {
                            for (SpartanPlayer player : staffOnline) {
                                statisticsArray.add("§c" + player.getName());
                            }
                        }
                    }
                }
            } else {
                players = 0.0;
            }
        }

        // Separator
        List<String> warnings = getWarnings(true);

        if (warnings.size() > 0) {
            if (showStatistics) {
                InventoryUtils.prepareDescription(array, "Important Information");
            } else {
                array.add("");
            }
            array.add("§4Warning§8:");

            for (String warning : warnings) {
                array.add("§c" + warning);
            }
            array.add("");
        } else if (showStatistics) {
            InventoryUtils.prepareDescription(array, "Important Information");
        }

        // Separator
        if (showStatistics) {
            if (players > 0.0) {
                array.add("§7Data from §4" + ((int) players) + " stored " + (players == 1 ? "player" : "players") + "§8:");
            } else {
                array.add("§7Data from §4stored players§8:");
            }

            if (statisticsArray.size() > 0) {
                array.addAll(statisticsArray);
            } else if (caching) {
                array.add("§7" + PlayerStateLists.calculatingData);
            } else {
                array.add("§7" + PlayerStateLists.noDataAvailable);
            }
        }

        // Separator
        return array;
    }

    // Separator

    public static List<String> getWarnings(boolean menu) {
        List<String> warnings = new LinkedList<>();

        if (CloudFeature.isServerLimited()) {
            warnings.add(menu ? prepareLore(CloudFeature.getMaximumServerLimitMessage()) : CloudFeature.getMaximumServerLimitMessage());
        }
        String outdatedVersionMessage = CloudFeature.getOutdatedVersionMessage();

        if (outdatedVersionMessage != null && outdatedVersionMessage.length() > 0) {
            warnings.add(menu ? prepareLore(outdatedVersionMessage) : outdatedVersionMessage);
        }
        return warnings;
    }

    public static boolean messageWarnings(SpartanPlayer p) {
        List<String> messages = new LinkedList<>(),
                warnings = getWarnings(false);
        int warningsCount = warnings.size();

        if (warningsCount > 0) {
            messages.add("");

            for (int position = 0; position < warningsCount; position++) {
                String color = (position + 1) % 2 == 0 ? "§7" : "§c";
                messages.add(color + prepareMessage(warnings.get(position), color));
            }
            messages.add(communicationDetails);

            // Separator
            String message = StringUtils.toString(messages.toArray(new String[0]), "\n");
            String hash = String.valueOf(Objects.hash(message));
            message = AwarenessNotifications.getNotification(message);

            if (AwarenessNotifications.canSend(p.getUniqueId(), hash)) {
                p.sendImportantMessage(message);
            }
            return true;
        }
        return false;
    }

    // Separator

    private static String prepareMessage(String s, String color) {
        return s.replace("§r", "§r" + color);
    }

    private static String prepareLore(String s) {
        return s.length() > StringUtils.idealDescriptionLimit ? s.substring(0, StringUtils.idealDescriptionLimit - 3) + "..." : s;
    }
}
