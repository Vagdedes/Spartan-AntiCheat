package com.vagdedes.spartan.utils.minecraft.entity;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.potion.PotionEffectType.*;

public class PlayerUtils {

    public static final boolean
            slowFall = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            dolphinsGrace = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            soulSpeed = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16),
            levitation = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            elytra = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            trident = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13);
    private static final Material
            gold_sword = MaterialUtils.get("gold_sword"),
            wood_sword = MaterialUtils.get("wood_sword"),
            gold_axe = MaterialUtils.get("gold_axe"),
            wood_axe = MaterialUtils.get("wood_axe"),
            gold_pickaxe = MaterialUtils.get("gold_pickaxe"),
            wood_pickaxe = MaterialUtils.get("wood_pickaxe"),
            diamond_spade = MaterialUtils.get("diamond_spade"),
            iron_spade = MaterialUtils.get("iron_spade"),
            gold_spade = MaterialUtils.get("gold_spade"),
            stone_spade = MaterialUtils.get("stone_spade"),
            wood_spade = MaterialUtils.get("wood_spade");

    public static final double
            optimizationY = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 0.005 : 0.003,
            airDrag = AlgebraUtils.floatDouble(0.98),
            waterDrag = AlgebraUtils.floatDouble(0.8),
            lavaDrag = 0.5,
            jumpAcceleration = AlgebraUtils.floatDouble(0.42),
            airAcceleration = 0.08,
            airAccelerationUnloaded = AlgebraUtils.floatDouble(0.098),
            slowFallAcceleration = 0.01,
            liquidAcceleration = 0.02,
            chunk = 16.0,
            climbingUpDefault = 0.12 * airDrag, // 0.11760
            climbingDownDefault = AlgebraUtils.floatDouble(0.15),
            honeyBlockDownDefault = AlgebraUtils.floatDouble(0.13) * airDrag,
            webBlockDownDefault = AlgebraUtils.floatDouble(0.64) * airDrag,
            maxJumpingMotionDifference;

    public static final int
            playerInventorySlots = (9 * 5) + 1,
            height,
            fallDamageAboveBlocks = 3;

    private static final Map<Byte, List<Double>> jumpsValues = new LinkedHashMap<>();
    private static final Map<Integer, Integer> fallTicks = new OverflowMap<>(
            new ConcurrentHashMap<>(),
            1024
    );
    private static final Map<PotionEffectType, Long> handledPotionEffects = new LinkedHashMap<>();

    static {
        handledPotionEffects.put(PotionEffectUtils.JUMP, AlgebraUtils.integerRound(TPS.maximum * 5L) * TPS.tickTime);
        handledPotionEffects.put(SPEED, AlgebraUtils.integerRound(TPS.maximum * 2L) * TPS.tickTime);

        if (dolphinsGrace) {
            handledPotionEffects.put(DOLPHINS_GRACE, AlgebraUtils.integerRound(TPS.maximum) * TPS.tickTime);
        }
        if (slowFall) {
            handledPotionEffects.put(SLOW_FALLING, 10L * TPS.tickTime);
        }
        if (levitation) {
            handledPotionEffects.put(LEVITATION, 10L * TPS.tickTime);
        }

        // Separator

        for (int jumpEffect = 0; jumpEffect < 256; jumpEffect++) {
            List<Double> jumps = new ArrayList<>();
            double jump = jumpAcceleration + (jumpEffect * 0.1);

            while (jump > 0.0) {
                jumps.add(jump);
                jump = (jump - airAcceleration) * airDrag;
            }
            jumpsValues.put((byte) jumpEffect, jumps);
        }
        Iterator<Double> iterator = jumpsValues.get((byte) 0).iterator();
        double maxJumpingMotionDifferenceLocal = Double.MIN_VALUE,
                previousJumpingMotion = iterator.next();

        while (iterator.hasNext()) {
            double currentMotion = iterator.next();
            maxJumpingMotionDifferenceLocal = Math.max(
                    previousJumpingMotion - currentMotion,
                    maxJumpingMotionDifferenceLocal
            );
            previousJumpingMotion = currentMotion;
        }
        maxJumpingMotionDifference = maxJumpingMotionDifferenceLocal;

        // Separator

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            List<World> worlds = Bukkit.getWorlds();

            if (!worlds.isEmpty()) {
                int max = 256;

                for (World world : worlds) {
                    max = Math.max(world.getMaxHeight(), max);
                }
                height = max;
            } else {
                height = 256;
            }
        } else {
            height = 256;
        }
    }

    // Enchantments

    public static int getDepthStriderLevel(SpartanPlayer p) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            return 0;
        }
        ItemStack b = p.getInstance().getInventory().getBoots();
        return b != null ? b.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) : 0;
    }

    // Jumping

    public static boolean isJumping(double d, int jump, double diff) {
        if (d > 0.0) {
            for (double value : jumpsValues.get((byte) jump)) {
                if (Math.abs(value - d) < diff) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean justJumped(double d, int jump, double diff) {
        if (d > 0.0) {
            for (double value : jumpsValues.get((byte) jump)) {
                if (Math.abs(value - d) < diff) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Double> getJumpMotions(int jump) {
        return jumpsValues.get((byte) jump);
    }

    public static double getJumpMotionSum(int jump) {
        return jumpsValues.get((byte) jump).stream().mapToDouble(Double::doubleValue).sum();
    }

    public static double getJumpMotion(int jump, int tick) {
        return jumpsValues.get((byte) jump).get(tick);
    }

    public static int getJumpTicks(int jump) {
        return getJumpMotions(jump).size();
    }

    // Falling

    public static int getFallTick(double d, double acceleration, double drag, double precision, int jump) {
        if (d < 0.0) {
            acceleration = Math.abs(acceleration);
            int key = (Double.hashCode(precision) * SpartanBukkit.hashCodeMultiplier)
                    + Double.hashCode(AlgebraUtils.cut(d, GroundUtils.maxHeightLength)),
                    maxTicks = AlgebraUtils.integerCeil(1.0 / (1.0 - drag)) * 10;
            key = (key * SpartanBukkit.hashCodeMultiplier) + Double.hashCode(acceleration);
            key = (key * SpartanBukkit.hashCodeMultiplier) + Double.hashCode(drag);
            key = (key * SpartanBukkit.hashCodeMultiplier) + jump;
            Integer ticks = fallTicks.get(key);

            if (ticks == null) {
                ticks = 0;
                double preD = 0.0;

                while (d < 0.0) {
                    preD = d;
                    d = (d / drag) + acceleration;
                    ticks++;

                    if (ticks > maxTicks) {
                        fallTicks.put(key, -1);
                        return -1;
                    }
                }

                if (ticks > 0) {
                    preD = Math.abs(preD);
                    boolean precisely = preD < precision;

                    if (!precisely && Math.abs(preD - (acceleration * drag)) >= precision) {
                        boolean found = false;

                        if (jump == 0) {
                            List<Double> motions = getJumpMotions(0);
                            double last = motions.get(motions.size() - 1);

                            if (Math.abs(last - d) < precision) {
                                found = true;
                            }
                        } else {
                            for (int i : new int[]{jump, 0}) {
                                List<Double> motions = getJumpMotions(i);
                                double last = motions.get(motions.size() - 1);

                                if (Math.abs(last - d) < precision) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found) {
                            ticks = -1;
                        }
                    } else if (precisely) {
                        ticks -= 1;
                    }
                } else {
                    ticks = -1;
                }
                fallTicks.put(key, ticks);
            }
            return ticks;
        }
        return -1;
    }

    public static double calculateTerminalVelocity(double drag, double acceleration) {
        return ((1.0 / (1.0 - drag)) * acceleration);
    }

    public static double calculateNextFallMotion(double motion,
                                                 double acceleration, double drag) {
        double terminalVelocity = calculateTerminalVelocity(acceleration, drag);

        if (motion >= -terminalVelocity) {
            return (motion + acceleration) * drag;
        } else {
            return Double.MIN_VALUE;
        }
    }

    // Inventory

    public static boolean isSpadeItem(Material m) {
        return m == diamond_spade
                || m == iron_spade
                || m == gold_spade
                || m == stone_spade
                || m == wood_spade
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_HOE;
    }

    public static boolean isPickaxeItem(Material m) {
        return m == Material.DIAMOND_PICKAXE
                || m == Material.IRON_PICKAXE
                || m == Material.STONE_PICKAXE
                || m == gold_pickaxe
                || m == wood_pickaxe
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_PICKAXE;
    }

    public static boolean isAxeItem(Material m) {
        return m == Material.DIAMOND_AXE
                || m == Material.IRON_AXE
                || m == Material.STONE_AXE
                || m == gold_axe
                || m == wood_axe
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_AXE;
    }

    public static boolean isSwordItem(Material type) {
        return type == Material.DIAMOND_SWORD
                || type == gold_sword
                || type == Material.IRON_SWORD
                || type == Material.STONE_SWORD
                || type == wood_sword
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && type == Material.NETHERITE_SWORD;
    }

    // Potion Effects

    public static int getPotionLevel(SpartanPlayer entity, PotionEffectType potionEffectType) {
        SpartanPotionEffect potionEffect = entity.getPotionEffect(
                potionEffectType,
                handledPotionEffects.getOrDefault(potionEffectType, 0L)
        );

        if (potionEffect != null) {
            return potionEffect.bukkitEffect.getAmplifier();
        } else {
            return -1;
        }
    }

}
