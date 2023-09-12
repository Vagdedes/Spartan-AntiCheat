package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerCombat {

    // Separator

    private final String name;

    // Use of memory based lists to help save performance when iterating in the object (no advantage when iterating globally)
    private final List<PlayerFight>
            currentFights = new CopyOnWriteArrayList<>(),
            pastFights = new CopyOnWriteArrayList<>();
    private PlayerFight currentFightCache;

    PlayerCombat(String name) {
        this.name = name;
        this.currentFightCache = null;
    }

    // Separator

    public void removeFight(PlayerFight fight, boolean store) {
        if (store) {
            storeFight(fight, true);
        }
        currentFights.remove(fight);
    }

    public void storeFight(PlayerFight fight, boolean refreshMenu) {
        if (name.length() > 0) { // If length is zero, this means this object is part of a temporary profile
            pastFights.add(fight);
            ResearchEngine.addFight(fight, refreshMenu);
        }
    }

    public void createFight(PlayerFight fight) {
        currentFights.add(fight);
    }

    public boolean hasEnoughFights() {
        return pastFights.size() >= 5;
    }

    public boolean hasFights() {
        return pastFights.size() > 0;
    }

    public boolean hasCurrentFight() {
        return currentFights.size() > 0;
    }

    // Separator

    public List<PlayerFight> getWins() {
        int size = pastFights.size();

        if (size > 0) {
            List<PlayerFight> fights = new ArrayList<>(size);

            for (PlayerFight fight : pastFights) {
                if (fight.getWinner().getName().equals(name)) {
                    fights.add(fight);
                }
            }
            return fights;
        }
        return new ArrayList<>(0);
    }

    public List<PlayerFight> getLoses() {
        int size = pastFights.size();

        if (size > 0) {
            List<PlayerFight> fights = new ArrayList<>(size);

            for (PlayerFight fight : pastFights) {
                if (fight.getLoser().getName().equals(name)) {
                    fights.add(fight);
                }
            }
            return fights;
        }
        return new ArrayList<>(0);
    }

    // Separator

    public List<PlayerFight> getPastFights() {
        return new ArrayList<>(pastFights);
    }

    public List<PlayerFight> getCurrentFights() {
        return new ArrayList<>(currentFights);
    }

    // Separator

    public void setWinnerAgainst(String opponentName) {
        List<PlayerFight> fights = getCurrentFights();

        if (fights.size() > 0) {
            for (PlayerFight fight : fights) {
                PlayerOpponent opponent1 = fight.getOpponent1();
                PlayerOpponent opponent2 = fight.getOpponent2();

                if (opponent1.getName().equals(opponentName) || opponent2.getName().equals(opponentName)) {
                    fight.setWinner(name);
                    break;
                }
            }
        }
    }

    public PlayerFight getCurrentFightByCache() {
        return currentFightCache != null && !currentFightCache.hasEnded() ? currentFightCache : null;
    }

    public PlayerFight getCurrentFight(SpartanPlayer opponent) {
        List<PlayerFight> fights = getCurrentFights();

        if (fights.size() > 0) {
            String opponentName = opponent.getName();

            // Cache Component
            for (PlayerFight fight : fights) {
                if (fight.getOpponent1().getName().equals(opponentName)
                        || fight.getOpponent2().getName().equals(opponentName)) {
                    return currentFightCache = fight;
                }
            }
        }

        // Live Component
        return currentFightCache = new PlayerFight(new PlayerOpponent(name), new PlayerOpponent(opponent));
    }

    // Separator

    public void runFights() {
        if (currentFights.size() > 0) {
            for (PlayerFight fight : currentFights) {
                if (fight.hasEnded()) {
                    fight.judge();
                }
            }
        }
    }

    public void endFights() {
        if (currentFights.size() > 0) {
            for (PlayerFight fight : currentFights) {
                fight.judge();
            }
        }
    }

    // Separator

    public double[] getHitRatioAverages() {
        List<PlayerFight> fights = getPastFights();
        int total = fights.size();

        if (total > 0) {
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : fights) {
                double value = fight.getHitRatio(fight.getOpponent(name));
                average += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new double[]{min, average / ((double) total), max};
        }
        return null;
    }

    public double[] getReachAverages() {
        int total = pastFights.size();

        if (total > 0) {
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                double value = fight.getOpponent(name)[0].getReachAverage();
                average += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new double[]{min, average / ((double) total), max};
        }
        return null;
    }

    public double[] getCPSAverages() { // Attention, added after initial algorithm (1)
        if (pastFights.size() > 0) {
            int total = 0;
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                double value = fight.getOpponent(name)[0].getMaxCPS();

                if (value > 0) {
                    total++;
                    average += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }

            if (total > 0) {
                return new double[]{min, average / ((double) total), max};
            }
        }
        return null;
    }

    public double[] getYawRateAverages() { // Attention, added after initial algorithm (3)
        if (pastFights.size() > 0) {
            int total = 0;
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                float value = fight.getOpponent(name)[0].getYawRateAverage();

                if (value > 0.0f) {
                    total++;
                    average += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }

            if (total > 0) {
                return new double[]{min, average / ((double) total), max};
            }
        }
        return null;
    }

    public double[] getPitchRateAverages() { // Attention, added after initial algorithm (3)
        if (pastFights.size() > 0) {
            int total = 0;
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                float value = fight.getOpponent(name)[0].getPitchRateAverage();

                if (value > 0.0f) {
                    total++;
                    average += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }

            if (total > 0) {
                return new double[]{min, average / ((double) total), max};
            }
        }
        return null;
    }

    public double[] getHitComboAverages() { // Attention, added after initial algorithm (4)
        if (pastFights.size() > 0) {
            int total = 0;
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                int value = fight.getOpponent(name)[0].getMaxHitCombo();

                if (value > 0) {
                    total++;
                    average += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }

            if (total > 0) {
                return new double[]{min, average / ((double) total), max};
            }
        }
        return null;
    }

    public double[] getHitTimeAverages() {
        int total = pastFights.size();

        if (total > 0) {
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                double value = fight.getOpponent(name)[0].getHitTimeAverage();
                average += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new double[]{min, average / ((double) total), max};
        }
        return null;
    }

    public double[] getDurationAverages() {
        int total = pastFights.size();

        if (total > 0) {
            double average = 0.0, min = Double.MAX_VALUE, max = 0.0;

            for (PlayerFight fight : pastFights) {
                double value = fight.getOpponent(name)[0].getDuration();
                average += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new double[]{min, average / ((double) total), max};
        }
        return null;
    }

    public double getAverageKills() {
        int total = pastFights.size();

        if (total > 0) {
            int count = 0;

            for (PlayerFight fight : pastFights) {
                if (fight.isKill()) {
                    count++;
                }
            }
            return count / ((double) total);
        }
        return -1.0;
    }

    public double getAverageWinLossRatio() {
        List<PlayerFight> fights = getPastFights();

        if (fights.size() > 0) {
            int wins = 0, loses = 0;

            for (PlayerFight fight : fights) {
                if (fight.getWinner().getName().equals(name)) {
                    wins++;
                } else {
                    loses++;
                }
            }
            return loses == 0 ? wins : wins / ((double) loses);
        }
        return -1.0;
    }

    // Separator

    public double getVelocityOccurrences(float desiredValue, int desiredCount, boolean vertical) {
        int size = pastFights.size();

        if (size > 0) {
            int valid = 0, total = 0;

            for (PlayerFight fight : pastFights) {
                for (Float[] collection :
                        (vertical ? fight.getOpponent(name)[0].getVelocity().getVerticalStorage() :
                                fight.getOpponent(name)[0].getVelocity().getHorizontalStorage())) {
                    if (collection.length == desiredCount) {
                        int individual = 0;
                        total++;

                        for (float value : collection) {
                            if (value == desiredValue) {
                                if (value == 0.0) { // Zero is a special value which we do not interfere with
                                    valid++;
                                    break; // Once per cycle
                                } else {
                                    individual++;

                                    if (individual > 1) {
                                        break; // Break loop when more than one occurrence has been identified. (Check * comment for more info)
                                    }
                                }
                            }
                        }

                        if (individual == 1) { // *Non-zero values can be found once per count, more times raises suspicions and aren't counted
                            valid++;
                        }
                    }
                }
            }

            if (total > 0) {
                return (valid / ((double) total)) * 100.0;
            }
        }
        return -1.0;
    }
}
