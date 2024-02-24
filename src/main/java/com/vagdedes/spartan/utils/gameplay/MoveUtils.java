package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class MoveUtils {

    public static final double[]
            jumping = new double[]{0.08307, 0.42f, 0.0001}, // Last one is jump value precision
            climbing = new double[]{0.03684, 0.07531, 0.11215, 0.11760, 0.12529, 0.15, 0.15444}; // Attention to 'climbingScaffoldingMax'

    public static final double
            postJumpPreFall = 0.00301,
            gravityAcceleration = 0.0784,
            terminalVelocity = 3.92,
            drag = gravityAcceleration / terminalVelocity,
            dragComplement = 1.0 - drag,
            highPrecision = 0.0019,
            lowPrecision = highPrecision * 2.0,
            nearMaxFallingMotion = 3.87,
            chunk = 16.0,
            climbingScaffoldingMax = climbing[3] + 0.2,
            maxJumpingMotionDifference;

    public static final int
            height,
            fallDamageBlocks = 4,
            chunkInt = AlgebraUtils.integerRound(chunk),
            jumpingMotions = 5;

    private static final Set<Double> jumps = new HashSet<>(jumpingMotions);
    private static final Map<Integer, Double> gravity = new LinkedHashMap<>();

    static {
        jumps.add(jumping[1]); // 0.41999
        jumps.add(0.33319);
        jumps.add(0.24813);
        jumps.add(0.16477);
        jumps.add(jumping[0]);
        Iterator<Double> iterator = jumps.iterator();
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
        double fall = 0.0;
        int counter = 0;

        while (true) {
            fall = (fall * dragComplement) + gravityAcceleration;

            if ((terminalVelocity - fall) < highPrecision) {
                break;
            }
            gravity.put(counter, fall);
            counter++;
        }

        // Separator

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            List<World> worlds = Bukkit.getWorlds();

            if (worlds.size() > 0) {
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

    // Base

    static SpartanLocation trackLocation(SpartanPlayer p) {
        SpartanLocation to = p.getLocation(),
                from = p.getFromLocation();

        if (from != null) {
            p.setCustomDistance(
                    to.distance(from),
                    AlgebraUtils.getHorizontalDistance(to, from),
                    to.getY() - from.getY()
            );
        }
        p.setFromLocation(to); // Always last
        return to;
    }

    // Falling

    public static int getFallingTick(double d) {
        if (d < 0.0) {
            d = (0.0 - d);
            double min = Double.MAX_VALUE;

            for (Map.Entry<Integer, Double> entry : gravity.entrySet()) {
                double newMin = Math.abs(entry.getValue() - d);

                if (newMin < min) {
                    min = newMin;
                } else {
                    if (min < gravityAcceleration) {
                        return entry.getKey();
                    } else {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    public static double getFallingMotion(int tick) {
        return gravity.get(Math.min(gravity.size() - 1, tick));
    }

    // Jumping

    public static double getJumpingPrecision(SpartanPlayer p) {
        return p.isBedrockPlayer() || PlayerData.hasJumpEffect(p) ? MoveUtils.highPrecision : MoveUtils.jumping[2];
    }

    public static boolean isJumping(double d, double diff, double offDiff) {
        if (d > (MoveUtils.jumping[0] - offDiff) && d < (0.42 + offDiff) && !GroundUtils.heightExists(d)) {
            double original = jumping[1];

            for (double value : jumps) {
                if (Math.abs(value - d) < (value == original ? (diff / 2.0) : diff)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static double[] getClimbingMotionEdges() {
        double max = 0.0;
        double min = Double.MAX_VALUE;

        for (double value : climbing) {
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        return new double[]{min, max};
    }

    // Limits

    public static double calculateLimit(SpartanPlayer entity, SpartanLocation location, double value, double divide, PotionEffectType potionEffectType) {
        int level;

        if (potionEffectType == PotionEffectType.SPEED) {
            level = PlayerData.getPotionLevel(entity, potionEffectType);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && !entity.isGliding() && !entity.isSwimming() && !entity.isCrawling()) {
                ItemStack boots = entity.getInventory().getBoots();
                Enchantment enchantment;

                if (boots != null && boots.containsEnchantment(enchantment = Enchantment.SOUL_SPEED)) {
                    SpartanLocation locationM1;

                    if (location.isBlock(Material.SOUL_SAND, BlockUtils.hitbox_max)
                            || location.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox_max)
                            || (locationM1 = location.clone().add(0, -1, 0)).isBlock(Material.SOUL_SAND, BlockUtils.hitbox_max)
                            || locationM1.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox_max)) {
                        entity.getCooldowns().add("move-utils=soul-speed-blocks", 20);
                    }

                    if (!entity.getCooldowns().canDo("move-utils=soul-speed-blocks")) {
                        if (level < 0) {
                            level = 0;
                        }
                        level += boots.getEnchantmentLevel(enchantment);
                    }
                }
            }
        } else {
            level = PlayerData.getPotionLevel(entity, potionEffectType);
        }

        if (level > -1) {
            value += (level + 1.0) * (value / divide);
        }
        return value;
    }

    // Scenario

    public static boolean isInWaterTunnel(SpartanPlayer p, SpartanLocation location) {
        return p.wasInLiquids() && PlayerData.hasDolphinsGraceEffect(p)
                && PlayerData.hasSoulSpeedEnchantment(p, location);
    }
}