package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.*;

public class PlayerUtils {

    public static final boolean
            slowFall = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            dolphinsGrace = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            soulSpeed = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16),
            levitation = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9);
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
            terminalVelocity = AlgebraUtils.floatDouble(3.92),
            jumpAcceleration = AlgebraUtils.floatDouble(0.42),
            airAcceleration = terminalVelocity * 0.02,
            liquidAcceleration = 0.02,
            chunk = 16.0,
            climbingDefault = 0.11760,
            climbingScaffoldingMax = climbingDefault + 0.2,
            maxJumpingMotionDifference;

    public static final int
            playerInventorySlots = (9 * 4) + 1,
            height,
            fallDamageAboveBlocks = 3;

    private static final Map<Byte, List<Double>> jumpsValues = new LinkedHashMap<>();
    private static final Map<Integer, Integer>
            fallTicks = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Collection<Double>> fallMotions = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<PotionEffectType, Integer> handledPotionEffects = new LinkedHashMap<>();

    static {
        handledPotionEffects.put(JUMP, 100);
        handledPotionEffects.put(SPEED, 60);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            handledPotionEffects.put(DOLPHINS_GRACE, 30);
            handledPotionEffects.put(SLOW_FALLING, 20);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            handledPotionEffects.put(LEVITATION, 10);
        }

        // Separator

        for (int jumpEffect = 0; jumpEffect < 255; jumpEffect++) {
            List<Double> jumps = new ArrayList<>();
            double jump = jumpAcceleration + (jumpEffect * 0.1);

            while (jump > 0.0) {
                jumps.add(jump);
                jump = (jump * airDrag) - airAcceleration;
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
        PlayerInventory inventory = p.getInventory();
        ItemStack b = inventory == null ? null : inventory.getBoots();
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

    public static int getFallTick(double d, double precision, int jump) {
        if (d < 0.0) {
            int key = (Double.hashCode(precision) * SpartanBukkit.hashCodeMultiplier)
                    + Double.hashCode(AlgebraUtils.cut(d, GroundUtils.maxHeightLength));
            key = (key * SpartanBukkit.hashCodeMultiplier) + jump;
            Integer ticks;

            synchronized (fallTicks) {
                ticks = fallTicks.get(key);

                if (ticks == null) {
                    ticks = 0;
                    double preD = 0.0;

                    while (d < 0.0) {
                        preD = d;
                        d = (d + airAcceleration) / airDrag;
                        ticks++;
                    }

                    if (ticks > 0) {
                        if (Math.abs(preD - airAcceleration) >= precision) {
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
                        }
                    } else {
                        ticks = -1;
                    }
                    fallTicks.put(key, ticks);
                }
            }
            return ticks;
        }
        return -1;
    }

    public static double calculateNextFallMotion(double motion,
                                                 double drag, double acceleration,
                                                 double terminalVelocity) {
        if (motion > terminalVelocity) {
            return (motion * drag) - acceleration;
        } else {
            return Double.MIN_VALUE;
        }
    }

    public static Collection<Double> calculateFallMotions(double motion,
                                                          double drag, double acceleration,
                                                          double terminalVelocity) {
        if (motion > terminalVelocity) {
            int key = Objects.hash(
                    AlgebraUtils.cut(motion, GroundUtils.maxHeightLength),
                    drag,
                    acceleration,
                    terminalVelocity
            );
            Collection<Double> data;

            synchronized (fallMotions) {
                data = fallMotions.get(key);
            }
            if (data != null) {
                return data;
            } else {
                data = new ArrayList<>();

                while (motion > terminalVelocity) {
                    motion = (motion * drag) - acceleration;
                    data.add(motion);
                }
                synchronized (fallMotions) {
                    fallMotions.put(key, data);
                }
                return data;
            }
        } else {
            return new ArrayList<>(0);
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

    // Collisions

    public static int getNearbyCollisions(SpartanPlayer player, SpartanLocation location) {
        List<Entity> entities = player.getNearbyEntities(1.0, 1.0, 1.0);

        if (!entities.isEmpty()) {
            int count = 0;

            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    if (CombatUtils.getWidthAndHeight(entity)[0] >= location.distance(entity.getLocation())) {
                        count++;
                    }
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                    BoundingBox boundingBox = entity.getBoundingBox();
                    double width = Math.max(
                            boundingBox.getMaxX() - boundingBox.getMinX(),
                            boundingBox.getMaxZ() - boundingBox.getMinZ()
                    );

                    if (width >= location.distance(entity.getLocation())) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    // Potion Effects

    public static int getPotionEffectExtraTime(PotionEffectType potionEffectType) {
        return handledPotionEffects.getOrDefault(potionEffectType, 0);
    }

    public static int getPotionLevel(SpartanPlayer entity, PotionEffectType potionEffectType) {
        SpartanPotionEffect potionEffect = entity.getPotionEffect(
                potionEffectType,
                getPotionEffectExtraTime(potionEffectType)
        );

        if (potionEffect != null) {
            return potionEffect.bukkitEffect.getAmplifier();
        } else {
            return -1;
        }
    }

}
