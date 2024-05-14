package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.List;

public class MovementProcessing {

    private static final boolean
            v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8),
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9);
    private static final Material
            MAGMA_BLOCK = MaterialUtils.get("magma"),
            WATER = MaterialUtils.get("water"),
            LAVA = MaterialUtils.get("lava");
    public static final int
            motionPrecision = 4,
            heightPrecision = 3,
            quantumPrecision = AlgebraUtils.integerRound(
                    Math.sqrt(((motionPrecision * motionPrecision) + (heightPrecision * heightPrecision)) / 2.0)
            );
    public static final double maxPrecisionHeightLengthRatio = 1.0 / Math.pow(10, heightPrecision);

    public static void run(SpartanPlayer player,
                           SpartanLocation to,
                           double vertical, double box) {
        GameMode current = player.getGameMode();

        if (current == GameMode.CREATIVE
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
            player.getTrackers().add(Trackers.TrackerType.GAME_MODE, (int) TPS.maximum);
        }

        // Separator

        if (!calculateLiquid(player, to)) {
            if (!calculateGroundCollision(player, vertical, box)) {
                calculateBouncing(player, to, vertical);
            }
        } else {
            calculateBouncing(player, to, vertical);
        }
        calculateExtremeCollision(player, to);

        // Separator

        ServerFlying.run(player);
    }

    private static boolean calculateGroundCollision(SpartanPlayer player,
                                                    double vertical, double box) {
        if (player.isOnGround()
                && player.isOnGroundDefault()
                && player.movement.getTicksOnAir() == 0
                && player.getVehicle() == null
                && vertical == 0.0
                && GroundUtils.collisionHeightExists(box)) {
            Trackers handlers = player.getTrackers();
            handlers.removeMany(Trackers.TrackerFamily.MOTION);
            handlers.removeMany(Trackers.TrackerFamily.VELOCITY);
            player.movement.removeLastLiquidTime();
            return true;
        } else {
            return false;
        }
    }

    private static void calculateExtremeCollision(SpartanPlayer player, SpartanLocation location) {
        if (v1_9) {
            Trackers handlers = player.getTrackers();

            if (!handlers.isDisabled(Trackers.TrackerType.EXTREME_COLLISION)) {
                handlers.disable(Trackers.TrackerType.EXTREME_COLLISION, 2); // Disable for the next tick
                int collisions = PlayerUtils.getNearbyCollisions(player, location);

                if (collisions > 10) {
                    handlers.add(Trackers.TrackerType.EXTREME_COLLISION, Math.min((int) (TPS.maximum * 2), collisions));
                }
            }
        }
    }

    private static void calculateBouncing(SpartanPlayer player, SpartanLocation location,
                                          double vertical) {
        if (v1_8 && vertical != 0.0) {
            if (BlockUtils.isSlime(player, location, 4)) {
                int time = (int) (TPS.maximum * 2);
                player.getTrackers().add(Trackers.TrackerType.BOUNCING_BLOCKS, time);
                player.getTrackers().add(Trackers.TrackerType.BOUNCING_BLOCKS, "slime", time);
            } else if (BlockUtils.isBed(player, location, 4)) {
                int time = (int) (TPS.maximum * 2);
                player.getTrackers().add(Trackers.TrackerType.BOUNCING_BLOCKS, time);
                player.getTrackers().add(Trackers.TrackerType.BOUNCING_BLOCKS, "bed", time);
            }
        }
    }

    // Separator

    private static boolean calculateLiquid(SpartanPlayer player, SpartanLocation location) {
        if (location.getBlock().isLiquidOrWaterLogged(false)) {
            player.movement.setLastLiquid(WATER);

            if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    && player.movement.isLowEyeHeight()) {
                player.movement.setArtificialSwimming();
            }
            calculateBubbleWater(player, location);
            return true;
        } else if (location.getBlock().isLiquid(LAVA)) {
            player.movement.setLastLiquid(LAVA);
            return true;
        } else {
            for (double i = 0.0; i < player.getEyeHeight(); i++) {
                for (SpartanLocation locationModified : location.getSurroundingLocations(BlockUtils.boundingBox, i, BlockUtils.boundingBox)) {
                    if (locationModified.getBlock().isLiquidOrWaterLogged(false)) {
                        player.movement.setLastLiquid(WATER);

                        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                                && player.movement.isLowEyeHeight()) {
                            player.movement.setArtificialSwimming();
                        }
                        calculateBubbleWater(player, locationModified);
                        return true;
                    } else if (locationModified.getBlock().isLiquid(LAVA)) {
                        player.movement.setLastLiquid(LAVA);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void calculateBubbleWater(SpartanPlayer player, SpartanLocation location) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            int blockY = location.getBlockY(), minY = BlockUtils.getMinHeight(player.getWorld());

            if (blockY > minY) {
                SpartanLocation locationModified = location.clone();

                for (int i = 0; i <= (blockY - minY); i++) {
                    int nonLiquid = 0;
                    Collection<SpartanLocation> locations = locationModified.clone().add(0, -i, 0).getSurroundingLocations(BlockUtils.boundingBox, 0, BlockUtils.boundingBox);

                    for (SpartanLocation loc : locations) {
                        SpartanBlock block = loc.getBlock();
                        Material type = block.material;

                        if (type == Material.SOUL_SAND) {
                            player.getTrackers().add(Trackers.TrackerType.BUBBLE_WATER, (int) TPS.maximum);
                            player.getTrackers().add(Trackers.TrackerType.BUBBLE_WATER, "soul-sand", (int) TPS.maximum);
                            break;
                        } else if (type == MAGMA_BLOCK) {
                            player.getTrackers().add(Trackers.TrackerType.BUBBLE_WATER, (int) TPS.maximum);
                            player.getTrackers().add(Trackers.TrackerType.BUBBLE_WATER, "magma-block", (int) TPS.maximum);
                            break;
                        } else if (BlockUtils.isSolid(type) && !block.waterLogged) {
                            nonLiquid++;

                            if (nonLiquid == locations.size()) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // Separator

    public static boolean canCheck(SpartanPlayer player,
                                   boolean elytra, boolean velocity, boolean flight) {
        if ((elytra || !player.getTrackers().has(Trackers.TrackerType.ELYTRA_USE))
                && (flight || !player.movement.isFlying())
                && (velocity || !player.getTrackers().has(Trackers.TrackerType.ABSTRACT_VELOCITY))
                && !Attributes.has(player, Attributes.GENERIC_MOVEMENT_SPEED)) {
            if (Compatibility.CompatibilityType.MYTHIC_MOBS.isFunctional()
                    || Compatibility.CompatibilityType.ITEMS_ADDER.isFunctional()) {
                List<Entity> entities = player.getNearbyEntities(
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance
                );

                if (!entities.isEmpty()) {
                    for (Entity entity : entities) {
                        if (MythicMobs.is(entity) || ItemsAdder.is(entity)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

}
