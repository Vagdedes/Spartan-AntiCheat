package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;

import java.util.Set;

public class PatternUtils {

    @SafeVarargs
    public static boolean collides(SpartanLocation location,
                                   boolean top,
                                   boolean aboveTop,
                                   double horizontalBox,
                                   Set<Material>... sets) {
        if (top) {
            double verticalBox;

            if (aboveTop) {
                verticalBox = location.player != null
                        ? location.player.getEyeHeight() + (location.getY() - location.getBlockY())
                        : 1.0;
            } else {
                verticalBox = 0.0;
            }
            return isBlockPattern(new double[][]{
                    {horizontalBox, 0.0, horizontalBox},
                    {horizontalBox, 1.0, horizontalBox, aboveTop ? 1.0 : 0.0},
                    {horizontalBox, verticalBox, horizontalBox, verticalBox > 2.0 ? 1.0 : 0.0}
            }, location, horizontalBox > 0.0, sets);
        } else {
            return isBlockPattern(new double[]{
                    horizontalBox, 0.0, horizontalBox
            }, location, horizontalBox > 0.0, sets);
        }
    }

    // Separator

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
