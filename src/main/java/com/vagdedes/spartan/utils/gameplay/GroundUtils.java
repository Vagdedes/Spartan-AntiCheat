package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.HashHelper;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Sniffer;

import java.util.*;

public class GroundUtils {

    private static final Set<Double> heights = new HashSet<>();
    private static final Map<Material, double[]> specificHeights = new LinkedHashMap<>();
    private static final Map<Material, Integer> specificHeightsHashes = new LinkedHashMap<>();

    private static final boolean
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            v1_15 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15),
            v1_20 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20);
    public static final double
            hitbox = 0.305,
            bedStep = 0.5625,
            maxPossibleStep = 0.9375, // Attention, serious usage
            minPossibleStep = 0.015625,  // Attention, serious usage
            maxPlayerStep = bedStep + 0.1;
    public static final int maxHeightLength;

    static final String
            inaccuracyKey = "ground-utils=utility-inaccuracy";
    private static final String
            setOnGroundKey = "ground-utils=set-on-ground",
            setOffGroundKey = "ground-utils=set-off-ground";

    static {
        heights.add(maxPlayerStep);
        heights.add(bedStep);
        heights.add(maxPossibleStep);
        heights.add(minPossibleStep);
        heights.add(0.1875);
        heights.add(0.8125);
        heights.add(0.375);
        heights.add(0.0625);
        heights.add(0.125);
        heights.add(0.4375);
        heights.add(0.875);
        heights.add(0.6875);
        heights.add(0.09375);
        heights.add(0.3125);
        heights.add(0.625);
        heights.add(0.0);
        heights.add(0.25);
        heights.add(0.75);
        heights.add(0.5);

        // Separator

        for (Material m : Material.values()) {

            if (BlockUtils.areSlabs(m) || BlockUtils.areStairs(m) || BlockUtils.areCobbleWalls(m) || BlockUtils.areDoors(m)
                    || BlockUtils.areFences(m) || BlockUtils.areFenceGates(m) || m == MaterialUtils.get("cake")
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.AZALEA || m == Material.FLOWERING_AZALEA)) {
                specificHeights.put(m, new double[]{0.0, 0.5});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && (m == Material.SCULK_SENSOR || m == Material.SCULK_SHRIEKER)) { // Attention: calibrated sensor is missing
                specificHeights.put(m, new double[]{0.5});
            } else if (BlockUtils.areHeads(m)) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.75});
            } else if (BlockUtils.areTrapdoors(m)) {
                specificHeights.put(m, new double[]{0.0, 0.1875, 0.5});
            } else if (m == MaterialUtils.get("end_portal_frame")) {
                specificHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("enchanting_table")) {
                specificHeights.put(m, new double[]{0.75});
            } else if (m == MaterialUtils.get("soil") || v1_9 && m == Material.getMaterial("GRASS_PATH") || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.DIRT_PATH) {
                specificHeights.put(m, new double[]{0.9375});
            } else if (BlockUtils.areFlowerPots(m) || BlockUtils.areCandles(m)) {
                specificHeights.put(m, new double[]{0.0, 0.375});
            } else if (BlockUtils.areCarpets(m)) {
                specificHeights.put(m, new double[]{0.0625});
            } else if (m == MaterialUtils.get("repeater_on") || m == MaterialUtils.get("repeater_off") || m == MaterialUtils.get("comparator_on") || m == MaterialUtils.get("comparator_off")) {
                specificHeights.put(m, new double[]{0.125});
            } else if (BlockUtils.areBeds(m)) {
                specificHeights.put(m, new double[]{bedStep});
            } else if (BlockUtils.areAnvils(m)) {
                specificHeights.put(m, new double[]{0.0, 0.25});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.MEDIUM_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.25, 0.8125});
            } else if (v1_9 && (m == Material.CHORUS_PLANT || m == Material.CHORUS_FLOWER)) {
                specificHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("daylight_detector_1") || m == MaterialUtils.get("daylight_detector_2")) {
                specificHeights.put(m, new double[]{0.375});
            } else if (m == Material.SNOW
                    || BlockUtils.areShulkerBoxes(m)
                    || BlockUtils.isInteractiveAndPassable(m)
                    || v1_9 && m == Material.END_ROD
                    || v1_15 && m == Material.HONEY_BLOCK
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && m == Material.MUD) {
                specificHeights.put(m, new double[]{-1.0});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.STONECUTTER) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.5625, maxPlayerStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LANTERN || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_LANTERN) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.5625, 0.4375, 0.625, maxPlayerStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.CAMPFIRE || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_CAMPFIRE) {
                specificHeights.put(m, new double[]{0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.BELL) {
                specificHeights.put(m, new double[]{0.0, 0.9375, 0.8125, 0.375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.COMPOSTER) {
                specificHeights.put(m, new double[]{0.0, 0.125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LECTERN) {
                specificHeights.put(m, new double[]{0.0, 0.125, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.GRINDSTONE) {
                specificHeights.put(m, new double[]{0.0, 0.8125, 0.875, 0.6875, 0.625, 0.5625, 0.75, maxPlayerStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.TURTLE_EGG) {
                specificHeights.put(m, new double[]{0.0, 0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.AMETHYST_CLUSTER) {
                specificHeights.put(m, new double[]{0.0, 0.4375, 0.8125});
            } else if (BlockUtils.areCandleCakes(m)) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.CONDUIT || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.POINTED_DRIPSTONE) {
                specificHeights.put(m, new double[]{0.0, 0.6875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.CHAIN) {
                specificHeights.put(m, new double[]{0.0, 0.59375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.LARGE_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.3125, 0.8125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.SMALL_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.1875, 0.75});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.POWDER_SNOW_CAULDRON || m == Material.WATER_CAULDRON || m == Material.LAVA_CAULDRON)) {
                specificHeights.put(m, new double[]{0.0, 0.25, 0.3125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.LILY_PAD || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.getMaterial("WATER_LILY")) {
                specificHeights.put(m, new double[]{0.09375, 0.015625});
            }
            switch (m) {
                case TRAPPED_CHEST:
                case CHEST:
                case ENDER_CHEST:
                    specificHeights.put(m, new double[]{0.875});
                    break;
                case COCOA:
                    specificHeights.put(m, new double[]{0.75});
                    break;
                case CAULDRON:
                    specificHeights.put(m, new double[]{0.0, 0.25, 0.3125});
                    break;
                case BREWING_STAND:
                    specificHeights.put(m, new double[]{0.125, 0.875});
                    break;
                case CACTUS:
                    specificHeights.put(m, new double[]{0.9375});
                    break;
                case HOPPER:
                    specificHeights.put(m, new double[]{0.0, 0.6875});
                    break;
                case SOUL_SAND:
                    specificHeights.put(m, new double[]{0.0, 0.875});
                    break;
            }
        }
        int maxHeightLengthLocal = 0;

        for (Map.Entry<Material, double[]> entry : specificHeights.entrySet()) {
            int hash = 1;

            for (double d : entry.getValue()) {
                maxHeightLengthLocal = Math.max(maxHeightLengthLocal, Double.toString(d).length() - 2); // 0.XXXXX
                hash = HashHelper.extendInt(hash, Double.hashCode(d));
            }
            specificHeightsHashes.put(entry.getKey(), hash);
        }
        maxHeightLength = maxHeightLengthLocal;
    }

    // Method

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double y, boolean liquid, boolean climbable) {
        boolean original = y == 0;
        Cooldowns cooldowns = p.getCooldowns();

        if (!cooldowns.canDo(setOffGroundKey)) {
            return false;
        }
        if (!cooldowns.canDo(setOnGroundKey)) {
            if (original) {
                p.setAirTicks(0);
            }
            return true;
        }
        if (original) {
            Double vertical = p.getNmsVerticalDistance(),
                    oldVertical = p.getPreviousNmsVerticalDistance();

            if (vertical == null
                    || oldVertical == null
                    || (vertical - oldVertical) < 0.0
                    && p.isFalling(vertical)
                    && p.isFalling(oldVertical)) {
                return false;
            }
            if (p.getHandlers().has(Handlers.HandlerType.Piston)
                    || stepsOnBoats(p)
                    || v1_9 && (stepsOnShulkers(p) || v1_20 && stepsOnSniffers(p))) {
                p.setAirTicks(0);
                return true;
            }
        }
        SpartanBlock block = loc.getBlock();

        if (liquid && block.isLiquid()
                || climbable && BlockUtils.canClimb(block.material)) {
            if (original) {
                p.setAirTicks(0);
            }
            return true;
        }
        original &= p.getVehicle() == null && !p.bedrockPlayer;
        double box = loc.getY() - loc.getBlockY();

        for (SpartanLocation loopLocation : loc.getSurroundingLocations(hitbox, 0, hitbox)) {
            for (double position : new double[]{-(box + minPossibleStep), 0.0}) {
                Material type = loopLocation.clone().add(0.0, position, 0.0).getBlock().material;

                if (BlockUtils.isSolid(type)) {
                    if (original) {
                        double[] heights = specificHeights.get(type);

                        if (heights != null) {
                            if (heights.length == 1) {
                                if (heights[0] == -1.0 || heights[0] == box) {
                                    p.setAirTicks(0);
                                    return true;
                                }
                            } else {
                                for (double height : heights) {
                                    if (height == box) {
                                        p.setAirTicks(0);
                                        return true;
                                    }
                                }
                            }
                        } else if (box == 0.0) {
                            p.setAirTicks(0);
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Runnables

    public static void setOnGround(SpartanPlayer p, int ticks) {
        p.getCooldowns().add(setOnGroundKey, ticks);
        p.setAirTicks(0);
    }

    public static void setOffGround(SpartanPlayer p, int ticks) {
        p.getCooldowns().add(setOffGroundKey, ticks);
    }

    // Utils

    public static Set<Double> getHeights(Material m) {
        double[] heights = specificHeights.get(m);

        if (heights != null) {
            HashSet<Double> set = new HashSet<>(heights.length);

            for (double height : heights) {
                set.add(height);
            }
            return set;
        } else {
            return new HashSet<>(0);
        }
    }

    public static double[] getHeightsRaw(Material m) {
        return specificHeights.getOrDefault(m, new double[]{0.0});
    }

    public static int getHeightsHash(Material m, int defaultValue) {
        return specificHeightsHashes.getOrDefault(m, defaultValue);
    }

    public static long getHeightsHashLong(Material m, long defaultValue) {
        Integer hash = specificHeightsHashes.get(m);
        return hash != null ? hash : defaultValue;
    }

    public static double getMaxHeight(Material m) {
        double[] heights = getHeightsRaw(m);

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

    public static boolean heightExists(double d) {
        return heights.contains(d);
    }

    // Entities

    public static boolean stepsOnBoats(SpartanPlayer p) {
        List<Entity> entities = p.getNearbyEntities(2.0, 2.0, 2.0);

        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity instanceof Boat) {
                    p.setAirTicks(0);
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
                    p.setAirTicks(0);
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
                    p.setAirTicks(0);
                    return true;
                }
            }
        }
        return false;
    }
}
