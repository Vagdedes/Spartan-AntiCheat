package com.vagdedes.spartan.utils.minecraft.world;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import org.bukkit.Material;

import java.util.Set;

public class BlockPatternUtils {

    @SafeVarargs
    public static boolean isBlockPattern(double[][] positions,
                                         SpartanLocation location,
                                         boolean surroundings,
                                         Set<Material>... sets) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    Material material = surroundingLocation.getBlock().material;

                    for (Set<Material> set : sets) {
                        if (set.contains(material)) {
                            return true;
                        }
                    }
                }
            } else {
                Material material = location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().material;

                for (Set<Material> set : sets) {
                    if (set.contains(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SafeVarargs
    public static boolean isBlockPattern(double[] coordinates,
                                         SpartanLocation location,
                                         boolean surroundings,
                                         Set<Material>... sets) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                Material material = surroundingLocation.getBlock().material;

                for (Set<Material> set : sets) {
                    if (set.contains(material)) {
                        return true;
                    }
                }
            }
        } else {
            Material material = location.clone().add(x, y, z).getBlock().material;

            for (Set<Material> set : sets) {
                if (set.contains(material)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Separator

    public static boolean isWaterLogPattern(double[][] positions,
                                            SpartanLocation location,
                                            boolean surroundings) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    if (surroundingLocation.getBlock().waterLogged) {
                        return true;
                    }
                }
            } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().waterLogged) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWaterLogPattern(double[] coordinates,
                                            SpartanLocation location,
                                            boolean surroundings) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                if (surroundingLocation.getBlock().waterLogged) {
                    return true;
                }
            }
            return false;
        } else {
            return location.clone().add(x, y, z).getBlock().waterLogged;
        }
    }

    // Separator

    public static boolean isWaterPattern(double[][] positions,
                                         SpartanLocation location,
                                         boolean surroundings) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    if (surroundingLocation.getBlock().isLiquidOrWaterLogged(false)) {
                        return true;
                    }
                }
            } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWaterPattern(double[] coordinates,
                                         SpartanLocation location,
                                         boolean surroundings) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                if (surroundingLocation.getBlock().isLiquidOrWaterLogged(false)) {
                    return true;
                }
            }
            return false;
        } else {
            return location.clone().add(x, y, z).getBlock().isLiquidOrWaterLogged(false);
        }
    }

    // Separator

    public static boolean isLavaPattern(double[][] positions,
                                        SpartanLocation location,
                                        boolean surroundings) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    if (surroundingLocation.getBlock().isLiquid(MaterialUtils.get("lava"))) {
                        return true;
                    }
                }
            } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquid(MaterialUtils.get("lava"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLavaPattern(double[] coordinates,
                                        SpartanLocation location,
                                        boolean surroundings) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                if (surroundingLocation.getBlock().isLiquid(MaterialUtils.get("lava"))) {
                    return true;
                }
            }
            return false;
        } else {
            return location.clone().add(x, y, z).getBlock().isLiquid(MaterialUtils.get("lava"));
        }
    }

    // Separator

    public static boolean isLiquidPattern(double[][] positions,
                                          SpartanLocation location,
                                          boolean surroundings) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    if (surroundingLocation.getBlock().isLiquidOrWaterLogged(true)) {
                        return true;
                    }
                }
            } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isLiquidOrWaterLogged(true)) {
                return false;
            }
        }
        return false;
    }

    public static boolean isLiquidPattern(double[] coordinates,
                                          SpartanLocation location,
                                          boolean surroundings) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                if (surroundingLocation.getBlock().isLiquidOrWaterLogged(true)) {
                    return true;
                }
            }
            return false;
        } else {
            return location.clone().add(x, y, z).getBlock().isLiquidOrWaterLogged(true);
        }
    }

    // Separator

    public static boolean isNonWaterLoggedLiquidPattern(double[][] positions,
                                                        SpartanLocation location,
                                                        boolean surroundings) {
        for (double[] coordinates : positions) {
            if (surroundings) {
                for (SpartanLocation surroundingLocation : location.getSurroundingLocations(coordinates[0], coordinates[1], coordinates[2])) {
                    if (surroundingLocation.getBlock().isNonWaterLoggedLiquid(true)) {
                        return true;
                    }
                }
            } else if (location.clone().add(coordinates[0], coordinates[1], coordinates[2]).getBlock().isNonWaterLoggedLiquid(true)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNonWaterLoggedLiquidPattern(double[] coordinates,
                                                        SpartanLocation location,
                                                        boolean surroundings) {
        double x = coordinates[0], y = coordinates[1], z = coordinates[2];

        if (surroundings) {
            for (SpartanLocation surroundingLocation : location.getSurroundingLocations(x, y, z)) {
                if (surroundingLocation.getBlock().isNonWaterLoggedLiquid(true)) {
                    return true;
                }
            }
            return false;
        } else {
            return location.clone().add(x, y, z).getBlock().isNonWaterLoggedLiquid(true);
        }
    }

}
