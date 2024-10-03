package com.vagdedes.spartan.utils.minecraft.world;

import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Sniffer;

import java.util.*;

public class GroundUtils {

    private static final List<Double>
            blockHeights = new ArrayList<>(),
            collisionHeights = new ArrayList<>();
    private static final Map<Material, double[]> correlatedBlockHeights = new LinkedHashMap<>();
    public static final Set<Material> abstractMaterials = new HashSet<>();

    private static final boolean
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            v1_15 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15),
            v1_20 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20);
    public static final double
            boundingBox = 0.3 + 0.005,
            contactBoundingBox = 0.62,
            maxBoundingBox = 0.9375, // Attention, serious usage
            minBoundingBox = 0.015625,  // Attention, serious usage
            maxPlayerStep = 0.5625,
            maxHeightLengthRatio;
    public static final int maxHeightLength;

    static {
        blockHeights.add(maxBoundingBox);
        blockHeights.add(minBoundingBox);
        blockHeights.add(maxPlayerStep);
        blockHeights.add(0.0);
        blockHeights.add(0.6625);
        blockHeights.add(0.1875);
        blockHeights.add(0.8125);
        blockHeights.add(0.375);
        blockHeights.add(0.0625);
        blockHeights.add(0.125);
        blockHeights.add(0.4375);
        blockHeights.add(0.875);
        blockHeights.add(0.6875);
        blockHeights.add(0.09375);
        blockHeights.add(0.3125);
        blockHeights.add(0.625);
        blockHeights.add(0.25);
        blockHeights.add(0.75);
        blockHeights.add(0.5);

        // Separator

        double playerHeightCeil = Math.ceil(CombatUtils.playerWidthAndHeight[1]);

        for (double height : blockHeights) {
            collisionHeights.add(
                    playerHeightCeil
                            + (height == 0.0 ? 1.0 : height)
                            - CombatUtils.playerWidthAndHeight[1]
            );
        }

        // Separator

        for (Material m : Material.values()) {
            if (BlockUtils.areSlabs(m) || BlockUtils.areStairs(m) || BlockUtils.areCobbleWalls(m) || BlockUtils.areDoors(m)
                    || BlockUtils.areFences(m) || BlockUtils.areFenceGates(m) || m == MaterialUtils.get("cake")
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.AZALEA || m == Material.FLOWERING_AZALEA)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.5});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && (m == Material.SCULK_SENSOR || m == Material.SCULK_SHRIEKER)) { // Attention: calibrated sensor is missing
                correlatedBlockHeights.put(m, new double[]{0.5});
            } else if (BlockUtils.areHeads(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.5, 0.75});
            } else if (BlockUtils.areTrapdoors(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.1875, 0.5});
            } else if (m == MaterialUtils.get("end_portal_frame")) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("enchanting_table")) {
                correlatedBlockHeights.put(m, new double[]{0.75});
            } else if (m == MaterialUtils.get("soil") || v1_9 && m == Material.getMaterial("GRASS_PATH") || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.DIRT_PATH) {
                correlatedBlockHeights.put(m, new double[]{0.9375});
            } else if (BlockUtils.areFlowerPots(m) || BlockUtils.areCandles(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.375});
            } else if (BlockUtils.areCarpets(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0625});
            } else if (m == MaterialUtils.get("repeater_on") || m == MaterialUtils.get("repeater_off") || m == MaterialUtils.get("comparator_on") || m == MaterialUtils.get("comparator_off")) {
                correlatedBlockHeights.put(m, new double[]{0.125});
            } else if (BlockUtils.areBeds(m)) {
                correlatedBlockHeights.put(m, new double[]{0.5625});
            } else if (BlockUtils.areAnvils(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.25});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.MEDIUM_AMETHYST_BUD) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.25, 0.8125});
            } else if (v1_9 && (m == Material.CHORUS_PLANT || m == Material.CHORUS_FLOWER)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("daylight_detector_1") || m == MaterialUtils.get("daylight_detector_2")) {
                correlatedBlockHeights.put(m, new double[]{0.375});
            } else if (m == Material.SNOW
                    || BlockUtils.areShulkerBoxes(m)
                    || BlockUtils.isInteractiveAndPassable(null, m)
                    || v1_9 && m == Material.END_ROD
                    || v1_15 && m == Material.HONEY_BLOCK
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && m == Material.MUD) {
                correlatedBlockHeights.put(m, new double[]{-1.0});
                abstractMaterials.add(m);
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.STONECUTTER) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.5, 0.5625, 0.6625});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LANTERN || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_LANTERN) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.5, 0.5625, 0.4375, 0.625, 0.6625});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.CAMPFIRE || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_CAMPFIRE) {
                correlatedBlockHeights.put(m, new double[]{0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.BELL) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.9375, 0.8125, 0.375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.COMPOSTER) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LECTERN) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.125, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.GRINDSTONE) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.8125, 0.875, 0.6875, 0.625, 0.5625, 0.75, 0.6625});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.TURTLE_EGG) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.AMETHYST_CLUSTER) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.4375, 0.8125});
            } else if (BlockUtils.areCandleCakes(m)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.5, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.CONDUIT || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.POINTED_DRIPSTONE) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.6875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.CHAIN) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.59375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.LARGE_AMETHYST_BUD) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.3125, 0.8125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.SMALL_AMETHYST_BUD) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.1875, 0.75});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.POWDER_SNOW_CAULDRON || m == Material.WATER_CAULDRON || m == Material.LAVA_CAULDRON)) {
                correlatedBlockHeights.put(m, new double[]{0.0, 0.25, 0.3125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.LILY_PAD || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.getMaterial("WATER_LILY")) {
                correlatedBlockHeights.put(m, new double[]{0.09375, 0.015625});
            }
            switch (m) {
                case TRAPPED_CHEST:
                case CHEST:
                case ENDER_CHEST:
                    correlatedBlockHeights.put(m, new double[]{0.875});
                    break;
                case COCOA:
                    correlatedBlockHeights.put(m, new double[]{0.75});
                    break;
                case CAULDRON:
                    correlatedBlockHeights.put(m, new double[]{0.0, 0.25, 0.3125});
                    break;
                case BREWING_STAND:
                    correlatedBlockHeights.put(m, new double[]{0.125, 0.875});
                    break;
                case CACTUS:
                    correlatedBlockHeights.put(m, new double[]{0.9375});
                    break;
                case HOPPER:
                    correlatedBlockHeights.put(m, new double[]{0.0, 0.6875});
                    break;
                case SOUL_SAND:
                    correlatedBlockHeights.put(m, new double[]{0.0, 0.875});
                    break;
            }
        }
        int maxHeightLengthLocal = 0;

        for (Map.Entry<Material, double[]> entry : correlatedBlockHeights.entrySet()) {

            for (double d : entry.getValue()) {
                maxHeightLengthLocal = Math.max(maxHeightLengthLocal, Double.toString(d).length() - 2); // 0.XXXXX
            }
        }
        maxHeightLength = maxHeightLengthLocal;
        maxHeightLengthRatio = 1.0 / Math.pow(10, maxHeightLength);
    }

    // Method

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc,
                                     boolean defaultOnGround,
                                     boolean checkEntities) {
        if (checkEntities
                && (p.trackers.has(PlayerTrackers.TrackerType.PISTON)
                || stepsOnBoats(p)
                || v1_9 && (stepsOnShulkers(p) || v1_20 && stepsOnSniffers(p)))) {
            return true;
        }
        if (loc != null) {
            double box = loc.getY() - loc.getBlockY(), distribution;

            if (defaultOnGround
                    && blockHeightExists(box)) {
                SpartanBlock block = loc.getBlock();

                if (block.isLiquidOrWaterLogged(true)
                        || BlockUtils.canClimb(block.getType(), false)) {
                    return true;
                }
            }
            Entity vehicle = p.getInstance().getVehicle();
            boolean hasVehicle = vehicle != null;

            if (hasVehicle) {
                distribution = Math.max(
                        CombatUtils.getWidthAndHeight(vehicle)[0],
                        boundingBox
                );
            } else {
                distribution = boundingBox;
            }
            for (double position : new double[]{-(box + minBoundingBox), 0.0, -maxPlayerStep}) {
                for (SpartanLocation loopLocation : loc.getSurroundingLocations(
                        distribution,
                        position,
                        distribution
                )) {
                    Material type = loopLocation.getBlock().getType();

                    if (BlockUtils.isSolid(type)) {
                        boolean abstractOnly = position == -maxPlayerStep;
                        double[] heights = correlatedBlockHeights.get(type);

                        if (heights != null) {
                            if (abstractOnly) {
                                if (heights[0] == -1.0 && box % minBoundingBox == 0.0) {
                                    return true;
                                }
                            } else if (heights.length == 1) {
                                if (heights[0] == -1.0 && box % minBoundingBox == 0.0
                                        || heights[0] == box) {
                                    return true;
                                }
                            } else {
                                for (double height : heights) {
                                    if (height == box) {
                                        return true;
                                    }
                                }
                            }
                        } else if (!abstractOnly && box == 0.0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Utils

    public static Collection<Double> getHeights() {
        return blockHeights;
    }

    public static double getMaxHeight(Material m) {
        double[] heights = correlatedBlockHeights.getOrDefault(m, new double[]{0.0});

        if (heights.length == 1) {
            return heights[0];
        } else {
            double max = 0.0;

            for (double height : heights) {
                if (height > max) {
                    max = height;
                }
            }
            return max;
        }
    }

    public static double getMinHeight(Material m) {
        double[] heights = correlatedBlockHeights.getOrDefault(m, new double[]{0.0});

        if (heights.length == 1) {
            return heights[0];
        } else {
            double min = Double.MAX_VALUE;

            for (double height : heights) {
                if (height < min) {
                    min = height;
                }
            }
            return min;
        }
    }

    public static boolean isAbstract(Material m) {
        return abstractMaterials.contains(m);
    }

    public static boolean blockHeightExists(double d) {
        return blockHeights.contains(d);
    }

    public static boolean collisionHeightExists(double d) {
        return collisionHeights.contains(d);
    }

    // Entities

    public static boolean stepsOnBoats(SpartanPlayer p) {
        List<Entity> entities = p.getNearbyEntities(2.0, 2.0, 2.0);

        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity instanceof Boat) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean stepsOnShulkers(SpartanPlayer p) {
        List<Entity> entities = p.getNearbyEntities(3.0, 3.0, 3.0);

        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity instanceof Shulker) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean stepsOnSniffers(SpartanPlayer p) {
        List<Entity> entities = p.getNearbyEntities(1.0, 1.0, 1.0);

        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity instanceof Sniffer && ((Sniffer) entity).getState() == Sniffer.State.IDLING) {
                    return true;
                }
            }
        }
        return false;
    }
}
