package com.vagdedes.spartan.utils.minecraft.entity;

import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class CombatUtils {

    public static final double maxHitDistance = 6.0;
    public static final double[] playerWidthAndHeight = new double[]{0.6, 1.8};

    public static EntityDamageEvent.DamageCause findDamageCause(String string) {
        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
            if (cause.name().equalsIgnoreCase(string)) {
                return cause;
            }
        }
        return null;
    }

    public static Vector getDirection(float yaw, float pitch) {
        double rotX = Math.toRadians((double) yaw),
                rotY = Math.toRadians((double) pitch),
                xz = Math.cos(rotY);
        return new Vector(
                -xz * Math.sin(rotX),
                -Math.sin(rotY),
                xz * Math.cos(rotX)
        );
    }

    public static double getRotationDistance(SpartanLocation location, Location targetLocation, boolean pitch) {
        double distance = location.distance(targetLocation);
        return location.clone().add(
                getDirection(location.getYaw(), pitch ? location.getPitch() : 0.0f).multiply(distance)
        ).distance(targetLocation) / distance;
    }

    public static double getRotationDistance(SpartanLocation location, SpartanLocation targetLocation, boolean pitch) {
        double distance = location.distance(targetLocation);
        return location.clone().add(
                getDirection(location.getYaw(), pitch ? location.getPitch() : 0.0f).multiply(distance)
        ).distance(targetLocation) / distance;
    }

    public static double[] getWidthAndHeight(Entity entity) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            BoundingBox boundingBox = entity.getBoundingBox();
            return new double[]{boundingBox.getMaxX() - boundingBox.getMinX(), entity.getHeight()};
        }
        EntityType entityType = entity.getType();

        if (entityType.toString().equals("PIG_ZOMBIE")) {
            return new double[]{0.6, 1.95};
        }
        switch (entityType.toString()) {
            case "SNOWMAN":
                return new double[]{0.7, 1.9};
            case "MUSHROOM_COW":
                return new double[]{0.9, 1.4};
        }
        switch (entityType) {
            case PLAYER:
                return playerWidthAndHeight;
            case ZOMBIE:
                return ((Zombie) entity).isBaby() ? new double[]{0.3, 0.975} : new double[]{0.6, 1.95};
            case VILLAGER:
            case WITCH:
                return new double[]{0.6, 1.95};
            case MAGMA_CUBE:
                int size = ((MagmaCube) entity).getSize();

                switch (size) {
                    case 3:
                        return new double[]{2.04, 2.04};
                    case 2:
                        return new double[]{1.02, 1.02};
                    default:
                        return new double[]{0.51, 0.51};
                }
            case SLIME:
                size = ((Slime) entity).getSize();

                switch (size) {
                    case 3:
                        return new double[]{2.04, 2.04};
                    case 2:
                        return new double[]{1.02, 1.02};
                    default:
                        return new double[]{0.51, 0.51};
                }
            case CREEPER:
                return new double[]{0.6, 1.7};
            case BLAZE:
                return new double[]{0.6, 1.8};
            case SKELETON:
                return new double[]{0.6, 1.99};
            case ENDERMAN:
                return new double[]{0.6, 2.9};
            case WITHER:
                return new double[]{0.9, 3.5};
            case GHAST:
                return new double[]{4.0, 4.0};
            case SQUID:
                return new double[]{0.8, 0.8};
            case BAT:
                return new double[]{0.5, 0.9};
            case SPIDER:
                return new double[]{1.4, 0.9};
            case CAVE_SPIDER:
                return new double[]{0.7, 0.5};
            case HORSE:
                return ((Horse) entity).isAdult() ? new double[]{1.3964, 0.8} : new double[]{0.6982, 0.8};
            case COW:
                return ((Cow) entity).isAdult() ? new double[]{0.9, 1.4} : new double[]{0.45, 0.7};
            case CHICKEN:
                return ((Chicken) entity).isAdult() ? new double[]{0.4, 0.7} : new double[]{0.2, 0.35};
            case WOLF:
                return ((Wolf) entity).isAdult() ? new double[]{0.6, 0.85} : new double[]{0.3, 0.425};
            case PIG:
                return ((Pig) entity).isAdult() ? new double[]{0.9, 0.9} : new double[]{0.45, 0.45};
            case SHEEP:
                return ((Sheep) entity).isAdult() ? new double[]{0.9, 1.3} : new double[]{0.45, 0.675};
            case GIANT:
                return new double[]{3.6, 11.7};
            case SILVERFISH:
                return new double[]{0.4, 0.3};
            case SNOW_GOLEM:
                return new double[]{0.7, 1.9};
            case IRON_GOLEM:
                return new double[]{1.4, 2.7};
            case OCELOT:
                return new double[]{0.6, 0.7};
            default:
                return new double[]{
                        0.0,
                        MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                                ? entity.getHeight()
                                : ((LivingEntity) entity).getEyeHeight()
                };
        }
    }

    public static String entityToString(Entity e) {
        return e.getType().toString().toLowerCase().replace("_", "-");
    }

    public static boolean hasBlockBehind(SpartanPlayer p, LivingEntity entity) {
        SpartanLocation location = p.movement.getLocation().clone();
        location.setPitch(0.0f);
        SpartanLocation multipliedLocation = new SpartanLocation(
                ProtocolLib.getLocation(entity).clone().add(location.getDirection().multiply(1.0))
        );

        if (BlockUtils.isSolid(multipliedLocation.getBlock().getType())) {
            return true;
        }
        double height = getWidthAndHeight(entity)[1];

        if (height > 1.0) {
            for (int position = 1; position < AlgebraUtils.integerCeil(height); position++) {
                // We add one instead of the position because we no longer clone the location
                if (BlockUtils.isSolid(multipliedLocation.add(0, 1, 0).getBlock().getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canCheck(SpartanProtocol protocol) {
        if (!protocol.spartan.movement.isLowEyeHeight() // Covers swimming & gliding
                && !protocol.spartan.movement.wasFlying()
                && protocol.spartan.getVehicle() == null
                && Attributes.getAmount(protocol, Attributes.GENERIC_ARMOR) == 0.0
                && Attributes.getAmount(protocol, Attributes.GENERIC_ATTACK_SPEED) == 0.0
                && Attributes.getAmount(protocol, Attributes.GENERIC_KNOCKBACK_RESISTANCE) == 0.0
                && Attributes.getAmount(protocol, Attributes.PLAYER_ENTITY_INTERACTION_RANGE) == 0.0) {
            GameMode gameMode = protocol.bukkit.getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE
                    || gameMode == GameMode.CREATIVE;
        } else {
            return false;
        }
    }

    public static boolean canCheck(SpartanProtocol protocol, LivingEntity entity) {
        return !protocol.bukkit.getName().equals(entity.getName())
                && !MythicMobs.is(entity)
                && !ItemsAdder.is(entity);
    }

}
