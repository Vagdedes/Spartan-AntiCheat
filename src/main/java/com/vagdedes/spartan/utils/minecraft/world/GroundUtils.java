package com.vagdedes.spartan.utils.minecraft.world;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import lombok.Getter;
import org.bukkit.Material;

import java.util.*;

public class GroundUtils {

    private static final List<Double>
            blockHeights = new ArrayList<>(),
            collisionHeights = new ArrayList<>();
    @Getter
    private static final Set<Material> abstractMaterials = new HashSet<>();

    private static final boolean
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            v1_15 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15);

    public static final double
            boundingBox = 0.3 + 0.005,
            maxBoundingBox = 0.9375,
            minBoundingBox = 0.015625,
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
            if (m == Material.SNOW
                    || BlockUtils.areShulkerBoxes(m)
                    || BlockUtils.isInteractiveAndPassable(null, m)
                    || v1_9 && m == Material.END_ROD
                    || v1_15 && m == Material.HONEY_BLOCK
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && m == Material.MUD) {
                abstractMaterials.add(m);
            }
        }

        // Separator

        int maxHeightLengthLocal = 0;

        for (double height : blockHeights) {
            maxHeightLengthLocal = Math.max(maxHeightLengthLocal, Double.toString(height).length() - 2); // 0.XXXXX
        }
        maxHeightLength = maxHeightLengthLocal;
        maxHeightLengthRatio = 1.0 / Math.pow(10, maxHeightLength);
    }

    // Utils

    public static Collection<Double> getHeights() {
        return blockHeights;
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

}
