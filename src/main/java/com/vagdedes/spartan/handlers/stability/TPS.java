package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Bukkit;

import java.util.*;

public class TPS {

    public static final long tickTime = 50L;
    public static final int tickTimeInteger = (int) tickTime;
    public static final double
            criticalDifference = 0.5,
            minimum = 18.0,
            maximum = 20.0,
            excellent = 19.5,
            good = 19.0,
            tickTimeDecimal = (double) tickTime;
    private static final Calculator calculator = MultiVersion.folia ? null : new Calculator();
    private static final Map<Integer, Calculator> calculators = MultiVersion.folia ? new LinkedHashMap<>() : null;
    private static final int minimumCounted = 100;

    // Object

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (MultiVersion.folia) {
                    List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                    int size = players.size();

                    if (size > 0) {
                        Set<Integer> processed = Collections.synchronizedSet(new HashSet<>(size));

                        for (SpartanPlayer player : players) {
                            SpartanBukkit.runTask(player, () -> {
                                synchronized (processed) {
                                    int hash = getHash(player);

                                    if (processed.add(hash)) {
                                        runCalculator(getCalculator(hash, true));
                                    }
                                }
                            });
                        }
                    } else {
                        calculators.clear();
                    }
                } else {
                    runCalculator(calculator);
                }
            }, 1L, 1L);
        }
    }

    // Scheduler

    private static int getHash(SpartanPlayer player) {
        SpartanLocation location = player.getLocation();
        int hash = SpartanBukkit.hashCodeMultiplier + player.getWorld().hashCode();
        hash = (SpartanBukkit.hashCodeMultiplier * hash) + location.getChunkX();
        return (SpartanBukkit.hashCodeMultiplier * hash) + location.getBlockZ();
    }

    // Utils

    private static Calculator getCalculator(int hash, boolean create) {
        return create ? calculators.computeIfAbsent(hash, c -> new Calculator()) :
                calculators.get(hash);
    }

    private static void runCalculator(Calculator calculator) {
        calculator.time = System.currentTimeMillis();
        calculator.tickCalculator[calculator.counter % calculator.tickCalculator.length] = System.currentTimeMillis();
        calculator.counter++;

        if (calculator.protection > 0) {
            calculator.protection--;
        }
    }

    public static void clear() {
        if (MultiVersion.folia) {
            calculators.clear();
        } else {
            calculator.reset();
        }
    }

    // Helpers

    public static void setProtection(SpartanPlayer player, int ticks) {
        if (!Config.settings.exists(Settings.permissionOption)) {
            Config.settings.setOption(Settings.permissionOption, false);
        }

        // Separator

        Calculator calculator;

        if (MultiVersion.folia) {
            calculator = getCalculator(getHash(player), true);
        } else {
            calculator = TPS.calculator;
        }

        if (ticks > calculator.protection) {
            calculator.protection = ticks;
        }
    }

    public static long getMillisecondsPassed(SpartanPlayer player) {
        if (MultiVersion.folia) {
            Calculator calculator = getCalculator(getHash(player), false);
            return calculator == null ? tickTime : System.currentTimeMillis() - calculator.time;
        } else {
            return System.currentTimeMillis() - calculator.time;
        }
    }

    public static long getTick(SpartanPlayer player) {
        if (MultiVersion.folia) {
            Calculator calculator = getCalculator(getHash(player), false);
            return calculator == null ? 0 : calculator.counter;
        } else {
            return calculator.counter;
        }
    }

    public static double get(SpartanPlayer player, boolean protection) {
        if (MultiVersion.folia) {
            if (player == null) {
                return Bukkit.getTPS()[0];
            } else {
                Calculator calculator = getCalculator(getHash(player), false);
                return calculator == null ? maximum : calculator.result(protection);
            }
        } else {
            return calculator.result(protection);
        }
    }

    // Base

    public static double get(SpartanPlayer player) {
        return get(player, true);
    }

    public static boolean areDropping(SpartanPlayer player) {
        if (MultiVersion.folia) {
            Calculator calculator = getCalculator(getHash(player), false);
            return calculator != null && (calculator.protection > 0

                    || (TestServer.isIdentified() || Config.settings.getBoolean(Settings.tpsProtectionOption))
                    && calculator.result(false) < minimum);
        } else {
            return calculator.protection > 0

                    || (TestServer.isIdentified() || Config.settings.getBoolean(Settings.tpsProtectionOption))
                    && calculator.result(false) < minimum;
        }
    }

    private static class Calculator {

        private final long[] tickCalculator;
        private long time;
        private int counter, protection;

        private Calculator() {
            protection = 0;
            counter = 0;
            tickCalculator = new long[600];
        }

        private double result(boolean protection) {
            if (counter < minimumCounted
                    || protection && !Config.settings.getBoolean(Config.settings.tpsProtectionOption)) {
                return maximum;
            }
            int target = (counter - 1 - minimumCounted) % tickCalculator.length;

            if (target < 0) {
                return maximum;
            }
            long elapsed = System.currentTimeMillis() - tickCalculator[target];
            double tps = minimumCounted / (elapsed / 1000.0);
            return counter % 100 == 0 ? AlgebraUtils.randomDouble(0.0, good) : Math.min(maximum, Math.max(tps, 0.0));
        }

        private void reset() {
            protection = 0;
            counter = 0;
            Arrays.fill(tickCalculator, 0L);
        }
    }

}
