package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.GroundCollision;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Liquid;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.ExtremeCollision;
import com.vagdedes.spartan.functionality.identifiers.simple.VehicleAccess;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.*;

public class PlayerUtils {

    private static final Material
            gold_axe = MaterialUtils.get("gold_axe"),
            wood_axe = MaterialUtils.get("wood_axe"),
            gold_pickaxe = MaterialUtils.get("gold_pickaxe"),
            wood_pickaxe = MaterialUtils.get("wood_pickaxe"),
            diamond_spade = MaterialUtils.get("diamond_spade"),
            iron_spade = MaterialUtils.get("iron_spade"),
            gold_spade = MaterialUtils.get("gold_spade"),
            stone_spade = MaterialUtils.get("stone_spade"),
            wood_spade = MaterialUtils.get("wood_spade");

    private static final double
            climbingMin = 0.03684,
            climbingMax = 0.15444,
            climbingDefault = 0.11760;
    public static final double
            postJumpPreFall = 0.00301,
            gravityAcceleration = 0.0784,
            terminalVelocity = 3.92,
            drag = gravityAcceleration / terminalVelocity,
            dragComplement = 1.0 - drag,
            actualTerminalVelocity = terminalVelocity * dragComplement,
            highPrecision = 0.0019,
            lowPrecision = highPrecision * 2.0,
            chunk = 16.0,
            climbingScaffoldingMax = climbingDefault + 0.2,
            totalJumpingMotion,
            maxJumpingMotionDifference;

    public static final double[]
            jumping = new double[]{0.08307, 0.42f, 0.0001}, // Last one is jump value precision
            climbing = new double[]{climbingMin, 0.07531, 0.11215, climbingDefault, 0.12529, 0.15, climbingMax},
            climbingMinMax = new double[]{climbingMin, climbingMax};

    public static final int
            height,
            fallDamageBlocks = 4,
            jumpingMotions = 5;

    private static final Set<Double> jumps = new HashSet<>(jumpingMotions);
    private static final Map<Integer, Double> gravity = new LinkedHashMap<>();
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

        jumps.add(jumping[1]);
        jumps.add(0.33319);
        jumps.add(0.24813);
        jumps.add(0.16477);
        jumps.add(jumping[0]);
        Iterator<Double> iterator = jumps.iterator();
        double maxJumpingMotionDifferenceLocal = Double.MIN_VALUE,
                previousJumpingMotion = iterator.next(),
                totalJumpingMotionLocal = 0.0;

        while (iterator.hasNext()) {
            double currentMotion = iterator.next();
            totalJumpingMotionLocal += currentMotion;
            maxJumpingMotionDifferenceLocal = Math.max(
                    previousJumpingMotion - currentMotion,
                    maxJumpingMotionDifferenceLocal
            );
            previousJumpingMotion = currentMotion;
        }
        maxJumpingMotionDifference = maxJumpingMotionDifferenceLocal;
        totalJumpingMotion = totalJumpingMotionLocal;

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

    public static void run(SpartanPlayer p, Player n, Collection<PotionEffect> potionEffects) {
        update(p, n, false);

        if (!p.isDead() && !p.isSleeping()) {
            Buffer buffer = p.getBuffer();
            SpartanLocation to = p.movement.getLocation(),
                    from = p.movement.getFromLocation();

            // Separator
            if (from != null) {
                p.movement.setCustomDistance(
                        to.distance(from),
                        AlgebraUtils.getHorizontalDistance(to, from),
                        to.getY() - from.getY()
                );
            }
            p.movement.setFromLocation(to);

            // Separator
            boolean ground = p.refreshOnGroundCustom(from);

            if (ground) {
                p.movement.setAirTicks(0);
                p.movement.setGroundTicks(p.movement.getTicksOnGround() + 1);
            } else {
                p.movement.setGroundTicks(0);
                p.movement.setAirTicks(p.movement.getTicksOnAir() + 1);
                Double nmsVerticalDistance = p.movement.getNmsVerticalDistance(),
                        old_NmsVerticalDistance = p.movement.getPreviousNmsVerticalDistance();

                if (nmsVerticalDistance != null
                        && old_NmsVerticalDistance != null
                        && nmsVerticalDistance < old_NmsVerticalDistance) {
                    p.movement.setFallingTicks(p.movement.getFallingTicks() + 1);
                } else {
                    p.movement.setFallingTicks(0);
                }
            }

            // Separator
            if (isUsingAnInventory(p, 0)) {
                buffer.increase("player-data=inventory-use", 1);
            } else {
                buffer.remove("player-data=inventory-use");
            }

            // Separator
            if (!potionEffects.isEmpty()) {
                for (PotionEffect potionEffect : potionEffects) {
                    PotionEffectType type = potionEffect.getType();

                    if (handledPotionEffects.containsKey(type)) {
                        getPotionLevel(p, type);
                    }
                }
            }
        }
    }

    public static void update(SpartanPlayer p, Player n, boolean heavy) {
        if (heavy) {
            if (!Liquid.runMove(p)) {
                if (!GroundCollision.run(p)) {
                    ExtremeCollision.run(p);
                    BouncingBlocks.runMove(p);
                }
            } else {
                ExtremeCollision.run(p);
                BouncingBlocks.runMove(p);
            }
        } else {
            if (n.isSprinting()) {
                p.movement.setSprinting(true);
                p.movement.setSneaking(false);
            } else {
                p.movement.setSprinting(false);
                p.movement.setSneaking(n.isSneaking());
            }
            p.setFallDistance(n.getFallDistance(), false);
            p.setWalkSpeed(n.getWalkSpeed());
            p.setFlySpeed(n.getFlySpeed());
            p.setEyeHeight(n.getEyeHeight());
            p.setUsingItem(n.isBlocking());

            // Separator

            GameMode current = p.getGameMode();

            if (current == GameMode.CREATIVE
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
                p.getHandlers().add(Handlers.HandlerType.GameMode, 60);
            }

            // Separator

            Entity entity = n.getVehicle();
            p.setVehicle(entity);

            if (entity != null) {
                p.getBuffer().remove(GroundUtils.inaccuracyKey);
                VehicleAccess.runEnter(p, entity, false);
            }
        }
    }

    // Enchantments

    public static int getDepthStriderLevel(SpartanPlayer p) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            return 0;
        }
        ItemStack b = p.getInventory().getBoots();
        return b != null ? b.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) : 0;
    }

    // Speed

    public static boolean hasSoulSpeedEnchantment(SpartanPlayer p, SpartanLocation location) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
            return false;
        }
        String key = "player-data=soul-speed-enchantment";
        Cooldowns cooldowns = p.getCooldowns();
        boolean b = false;

        if (!cooldowns.canDo(key)) {
            if (cooldowns.get(key) > 5) {
                return true;
            } else {
                b = true;
            }
        }
        if (location != null) {
            ItemStack boots = p.getInventory().getBoots();

            if (boots != null && boots.containsEnchantment(Enchantment.SOUL_SPEED)) {
                SpartanLocation locationM1;

                if (location.isBlock(Material.SOUL_SAND, BlockUtils.hitbox)
                        || location.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox)
                        || (locationM1 = location.clone().add(0, -1, 0)).isBlock(Material.SOUL_SAND, BlockUtils.hitbox)
                        || locationM1.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox)) {
                    b = true;
                }
            }

            if (b) {
                p.getCooldowns().add(key, 20);
            }
        }
        return b;
    }

    public static boolean hasSpeedEffect(SpartanPlayer p, SpartanLocation location, boolean soulSpeed) {
        return p.getVehicle() == null
                && (getPotionLevel(p, PotionEffectType.SPEED) > 0
                || soulSpeed && hasSoulSpeedEnchantment(p, location));
    }

    // Jumping

    public static boolean hasJumpEffect(SpartanPlayer p) {
        return getPotionLevel(p, JUMP) > 0;
    }

    public static boolean hasLowJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = getPotionLevel(p, JUMP);
        return potionLevel > 0 && potionLevel <= 128
                || potionLevel >= 250;
    }

    public static boolean hasHighJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = getPotionLevel(p, JUMP);
        return potionLevel > 128
                && potionLevel < 250;
    }

    public static double getJumpingPrecision(SpartanPlayer p) {
        return p.bedrockPlayer || hasJumpEffect(p)
                ? highPrecision
                : jumping[2];
    }

    public static boolean isJumping(double d, double diff, double offDiff) {
        if (d > (jumping[0] - offDiff)
                && d < (0.42 + offDiff)
                && !GroundUtils.heightExists(d)) {
            Iterator<Double> iterator = jumps.iterator();
            double value = iterator.next();

            if (Math.abs(value - d) < (diff / 2.0)) { // More strict with the first motion
                return true;
            }
            while (iterator.hasNext()) {
                value = iterator.next();

                if (Math.abs(value - d) < diff) {
                    return true;
                }
            }
        }
        return false;
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
        return gravity.getOrDefault(tick, -1.0);
    }

    // Handled Effects

    public static boolean isInWaterTunnel(SpartanPlayer p, SpartanLocation location) {
        return p.movement.wasInLiquids()
                && hasDolphinsGraceEffect(p)
                && hasSoulSpeedEnchantment(p, location);
    }

    public static boolean hasDolphinsGraceEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && p.getVehicle() == null
                && getPotionLevel(p, PotionEffectType.DOLPHINS_GRACE) > 0;
    }

    public static boolean hasSlowFallingEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && p.getVehicle() == null
                && getPotionLevel(p, PotionEffectType.SLOW_FALLING) > 0;
    }

    public static boolean hasLevitationEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && getPotionLevel(p, PotionEffectType.LEVITATION) > 0;
    }

    public static boolean hasBadPotionEffects(SpartanPlayer p) {
        return p.hasPotionEffect(new PotionEffectType[]{
                PotionEffectType.POISON,
                PotionEffectType.HARM
        });
    }

    // Inventory

    public static boolean isUsingAnInventory(SpartanPlayer p, int limit) {
        return (limit == 0 || p.getBuffer().get("player-data=inventory-use") >= limit)
                && p.getOpenInventory().slots > 46;
    }

    public static boolean isUsable(Material type) {
        return CombatUtils.isSword(type)
                || CombatUtils.isBow(type)
                || type.isEdible()
                || type == Material.FISHING_ROD
                || type == Material.POTION;
    }

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

    // Movement

    public static double calculateLimit(SpartanPlayer entity, SpartanLocation location, double value, double divide, PotionEffectType potionEffectType) {
        int level;

        if (potionEffectType == PotionEffectType.SPEED) {
            level = getPotionLevel(entity, potionEffectType);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                    && !entity.movement.isGliding()
                    && !entity.movement.isSwimming()
                    && !entity.movement.isCrawling()) {
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
            level = getPotionLevel(entity, potionEffectType);
        }

        if (level > -1) {
            value += (level + 1.0) * (value / divide);
        }
        return value;
    }

    public static float getWalkSpeedDifference(SpartanPlayer p) {
        String key = "player-data=walk-difference";
        float difference = Math.max(p.getWalkSpeed() - 0.2f, !p.movement.isFlying() ? 0.0f : (p.getFlySpeed() - 0.1f));

        if (difference > 0.0f) {
            p.getCooldowns().add(key, 2 * 20);
            p.getDecimals().set(key, difference);
        } else if (!p.getCooldowns().canDo(key)) {
            return (float) p.getDecimals().get(key, 1.0);
        }
        return Math.max(difference, 0.0f);
    }

    // Collisions

    public static double getNearbyCollisions(SpartanPlayer p) {
        int max = 30;
        List<Entity> entities = p.getNearbyEntities(1.0, 1.0, 1.0);

        if (!entities.isEmpty()) {
            SpartanLocation location = p.movement.getLocation();
            int count = 0;

            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    if (CombatUtils.getWidthAndHeight(entity)[0] >=
                            AlgebraUtils.getHorizontalDistance(location, entity.getLocation())) {
                        count++;
                    }
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                    BoundingBox boundingBox = entity.getBoundingBox();
                    double width = Math.max(
                            boundingBox.getMaxX() - boundingBox.getMinX(),
                            boundingBox.getMaxZ() - boundingBox.getMinZ()
                    );

                    if (width >= AlgebraUtils.getHorizontalDistance(location, entity.getLocation())) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
            return count / ((double) max);
        }
        return 0.0;
    }

    public static boolean hasNearbyCollisions(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && (p.getHandlers().has(Handlers.HandlerType.ExtremeCollision)
                || getNearbyCollisions(p) > 0.0);
    }

    // Potion Effects

    public static int getPotionLevel(SpartanPlayer entity, PotionEffectType potionEffectType) {
        Entity vehicle = entity.getVehicle();
        String key = "player-data=potion-effect=" + potionEffectType.getName() + (vehicle != null ? "=" + vehicle.getEntityId() : "");
        int extraTicks = handledPotionEffects.getOrDefault(potionEffectType, 0);
        PotionEffect potionEffect = entity.getPotionEffect(potionEffectType);

        if (potionEffect != null) {
            int amplifier = potionEffect.getAmplifier();

            if (amplifier < 0) {
                return 0;
            }
            amplifier += 1;

            if (extraTicks > 0) {
                entity.getDecimals().set(key, amplifier);
                entity.getCooldowns().add(key, extraTicks);
            }
            return amplifier;
        }
        if (extraTicks > 0) {
            return !entity.getCooldowns().canDo(key) ? (int) entity.getDecimals().get(key, 0.0) : 0;
        }
        return 0;
    }

    // Ground

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double x, double y, double z) {
        return x == 0.0 && y == 0.0 && z == 0.0 && loc.player != null
                ? loc.player.isOnGroundCustom()
                : GroundUtils.isOnGround(p, loc.clone().add(x, y, z), y, true, true);
    }

}
