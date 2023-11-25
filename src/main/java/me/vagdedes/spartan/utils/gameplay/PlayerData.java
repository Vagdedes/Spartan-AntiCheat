package me.vagdedes.spartan.utils.gameplay;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.GroundCollision;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.ExtremeCollision;
import me.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.potion.PotionEffectType.*;

public class PlayerData {

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
    public static final long combatTimeRequirement = 2_500L;
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
    }

    public static void run(SpartanPlayer p, Player n, Collection<PotionEffect> potionEffects) {
        update(p, n, false);

        if (p.canDoAccurately() || p.isFlying()) {
            Buffer buffer = p.getBuffer();

            // Separator
            SpartanLocation from = p.getFromLocation();
            MoveUtils.trackLocation(p);
            boolean ground = p.refreshOnGroundCustom(from);

            if (ground) {
                p.setAirTicks(0);
                p.setGroundTicks(p.getTicksOnGround() + 1);
            } else {
                p.setGroundTicks(0);
                p.setAirTicks(p.getTicksOnAir() + 1);

                if (p.getNmsVerticalDistance() < p.getOld_NmsVerticalDistance()) {
                    p.setFallingTicks(p.getFallingTicks() + 1);
                } else {
                    p.setFallingTicks(0);
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
                p.setSprinting(true);
                p.setSneaking(false);
            } else {
                p.setSprinting(false);
                p.setSneaking(n.isSneaking());
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

    // Combat

    public static boolean isInActivePlayerCombat(SpartanPlayer p) {
        boolean damage = false, hit = false;
        List<PlayerFight> fights = p.getProfile().getCombat().getCurrentFights();

        if (!fights.isEmpty()) {
            double globalAverageReach = ResearchEngine.getAverageReach();

            for (PlayerFight fight : fights) {
                PlayerOpponent[] opponents = fight.getOpponent(p);

                if (!damage && opponents[0].getLastDamage() <= combatTimeRequirement) { // Last-damage double of last-hit due to advantage restrictions
                    if (hit) {
                        return true;
                    } else {
                        damage = true;
                    }
                }
                if (!hit && (opponents[0].getLastHit() <= combatTimeRequirement
                        || globalAverageReach != -1.0 && opponents[1].getReachAverage() > globalAverageReach)) {
                    if (damage) {
                        return true;
                    } else {
                        hit = true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInActiveEntityCombat(SpartanPlayer p) {
        return receivedEntityDamage(p) && hitAnEntity(p);
    }

    public static boolean isFightParticipant(SpartanPlayer p) {
        return p.getProfile().getCombat().hasCurrentFight();
    }

    public static boolean receivedPlayerDamage(SpartanPlayer p) {
        List<PlayerFight> fights = p.getProfile().getCombat().getCurrentFights();

        if (!fights.isEmpty()) {
            for (PlayerFight fight : fights) {
                if (fight.getOpponent(p)[0].getLastDamage() <= combatTimeRequirement) { // Last-damage double of last-hit due to advantage restrictions
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean receivedEntityDamage(SpartanPlayer p) {
        return Damage.getLastReceived(p) <= combatTimeRequirement;
    }

    public static boolean hitAPlayer(SpartanPlayer p) {
        List<PlayerFight> fights = p.getProfile().getCombat().getCurrentFights();

        if (!fights.isEmpty()) {
            for (PlayerFight fight : fights) {
                if (fight.getOpponent(p)[0].getLastHit() <= combatTimeRequirement) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hitAnEntity(SpartanPlayer p) {
        return Damage.getLastDealt(p) <= combatTimeRequirement;
    }

    // Enchantments

    public static int getDepthStriderLevel(SpartanPlayer p) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            return 0;
        }
        ItemStack b = p.getInventory().getBoots();
        return b != null ? b.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) : 0;
    }

    // Speed Effect

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

    public static boolean hasSpeedEffect(SpartanPlayer p, SpartanLocation location) {
        return hasSpeedEffect(p, location, true);
    }

    // Jump Effect

    public static boolean hasJumpEffect(SpartanPlayer p) {
        return PlayerData.getPotionLevel(p, JUMP) > 0;
    }

    public static boolean hasLowJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = PlayerData.getPotionLevel(p, JUMP);
        return potionLevel > 0 && potionLevel <= 128
                || potionLevel >= 250;
    }

    public static boolean hasHighJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = PlayerData.getPotionLevel(p, JUMP);
        return potionLevel > 128
                && potionLevel < 250;
    }

    // Handled Effects

    public static boolean hasConduitPowerEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && p.hasPotionEffect(PotionEffectType.CONDUIT_POWER)
                && p.wasInLiquids();
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
                && p.getOpenInventory().countSlots() > 46;
    }

    public static boolean isUsingTheCursor(SpartanPlayer p) {
        return p.getOpenInventory().getCursor().getType() != Material.AIR;
    }

    public static boolean holdsFood(SpartanPlayer p) {
        ItemStack item = p.getItemInHand();
        return item != null && item.getType().isEdible();
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

    public static boolean hasItemInHands(SpartanPlayer p, Material Material) {
        ItemStack item = p.getItemInHand();

        if (item.getType() == Material) {
            return true;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            ItemStack item_off = p.getInventory().getItemInOffHand();

            if (item_off.getType() == Material) {
                return true;
            }
        }
        return false;
    }

    // Movement

    public static float getWalkSpeedDifference(SpartanPlayer p) {
        String key = "player-data=walk-difference";
        float difference = Math.max(p.getWalkSpeed() - 0.2f, !p.isFlying() ? 0.0f : (p.getFlySpeed() - 0.1f));

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
            SpartanLocation location = p.getLocation();
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
                Buffer buffer = entity.getBuffer();
                buffer.set(key, amplifier);
                buffer.setRemainingTicks(key, extraTicks);
            }
            return amplifier;
        }
        if (extraTicks > 0) {
            Buffer buffer = entity.getBuffer();
            return buffer.getRemainingTicks(key) > 0 ? buffer.get(key, 0) : 0;
        }
        return 0;
    }

    // Entities

    public static int getEntitiesNumber(SpartanPlayer p, double distance, boolean findPusher) {
        int i = 0;
        boolean pusherFound = false;

        for (Entity e : p.getNearbyEntities(distance, distance, distance)) {
            if (e instanceof LivingEntity && (!(e instanceof Player) || p.canSee((Player) e))) {
                if (findPusher && (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && e.getPassengers().size() > 0 || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && e.getPassenger() != null)) {
                    pusherFound = true;
                } else {
                    i++;
                }
            } else if (e instanceof Boat) {
                i++;
            }
        }
        return pusherFound ? Integer.MAX_VALUE : i;
    }

    // Ground

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double x, double y, double z) {
        return x == 0.0 && y == 0.0 && z == 0.0 ?
                (p.isOnGroundCustom() || isOnGround(p, loc.clone().add(x, y, z), y))
                : isOnGround(p, loc.clone().add(x, y, z), y);
    }

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double y) {
        return GroundUtils.isOnGround(p, loc, y, true, true);
    }


}
