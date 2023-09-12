package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.checks.combat.VelocityCheck;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class PlayerVelocity {

    public static final int
            minimumCollection = 3,
            maximumCollection = 20,
            decimalPoints = 5;

    // Separator

    private final LinkedList<Float> verticalCalculator, horizontalCalculator;
    private final Map<Integer, Float[]> verticalStorage, horizontalStorage;
    private long timeAllowance;
    private SpartanLocation oldLocation;

    // Separator

    public PlayerVelocity(Map<Integer, Float[]> verticalStorage,
                          Map<Integer, Float[]> horizontalStorage) { // Used for logs
        this.verticalCalculator = new LinkedList<>();
        this.horizontalCalculator = new LinkedList<>();
        this.verticalStorage = verticalStorage;
        this.horizontalStorage = horizontalStorage;
        this.timeAllowance = 0L;
        this.oldLocation = null;
    }

    public PlayerVelocity(boolean empty) { // Used for memory
        this(empty ? null : new LinkedHashMap<>(), empty ? null : new LinkedHashMap<>());
    }

    // Separator

    public long getTimeAllowance() {
        return timeAllowance;
    }

    public void allowCollection(SpartanPlayer player, int entities) {
        if (entities == 1) {
            reset(player, true); // Try storing in case hits overlap each other
            this.timeAllowance = System.currentTimeMillis()
                    + (Math.min(player.getMaximumNoDamageTicks(), maximumCollection) * 50L) + 5L; // Add 5ms in the end for better accuracy
        } else {
            if (verticalCalculator.size() >= minimumCollection
                    || horizontalCalculator.size() >= minimumCollection) {
                runDetections(player);
            }
            reset(null, false);
        }
    }

    public static boolean canCalculateHorizontal(SpartanPlayer player) {
        return !player.hasPotionEffect(PotionEffectType.JUMP)
                && !player.hasPotionEffect(PotionEffectType.SPEED)
                && !PlayerData.hasLevitationEffect(player);
    }

    public static boolean canCalculateVertical(SpartanPlayer player) {
        return !PlayerData.hasSlowFallingEffect(player) && !PlayerData.hasLevitationEffect(player);
    }

    public void collect(SpartanPlayer player, SpartanLocation location) {
        boolean hasTimeAllowance = timeAllowance != 0L;

        if (VelocityCheck.canDo(player, false)) { // Check if the algorithm can collect data (usually after hits)
            if (hasTimeAllowance) {
                float blockY = (float) location.getY();

                if (System.currentTimeMillis() >= timeAllowance // Collection ticks have run out
                        || blockY <= BlockUtils.getMinHeight(player.getWorld()) // Falling in void
                        //|| player.isOnGroundCustom() && player.isOnGround() // Is standing on the ground
                        || player.getBlocksOffGround(MoveUtils.chunkInt + 1, true, true) > MoveUtils.chunk) { // Has moved to a high-ground area
                    reset(player, true);
                } else {
                    if (oldLocation != null) {
                        double verticalDifference = blockY - oldLocation.getBlockY(),
                                horizontalDifference = AlgebraUtils.getHorizontalDistance(location, oldLocation);

                        if (verticalDifference == 0.0
                                && (verticalCalculator.size() >= minimumCollection
                                || horizontalCalculator.size() >= minimumCollection)) { // Stop the collection when verticalDifference is zero and the least minimum collection is reached
                            reset(player, true);
                        } else {
                            if (canCalculateVertical(player)) {
                                verticalCalculator.add((float) AlgebraUtils.cut(verticalDifference, decimalPoints));
                            } else {
                                verticalCalculator.clear();
                            }
                            if (canCalculateHorizontal(player)) {
                                horizontalCalculator.add((float) AlgebraUtils.cut(horizontalDifference, decimalPoints));
                            } else {
                                horizontalCalculator.clear();
                            }
                        }
                    }
                    this.oldLocation = location; // Update block after comparison to further calculate
                }
            }
        } else if (hasTimeAllowance) {
            reset(null, false);
        }
    }

    // Separator

    private void runDetections(SpartanPlayer player) {
        if (!ResearchEngine.isCaching()) {
            VelocityCheck.run(player, verticalCalculator, horizontalCalculator);
        }
    }

    void reset(SpartanPlayer player, boolean store) {
        boolean clear = false;
        this.timeAllowance = 0L; // Always first to stop immediate future collection
        this.oldLocation = null; // Clear the old-location data for the next calculation cycle

        if (store) {
            runDetections(player);

            // Store vertical calculations when they are over 50% of the expected
            if (verticalStorage != null) {
                clear = true;
                int size = verticalCalculator.size();

                if (size >= minimumCollection) {
                    this.verticalStorage.put(size, verticalCalculator.toArray(new Float[0]));
                }
            }

            // Store horizontal calculations when they are over 50% of the expected
            if (horizontalStorage != null) {
                clear = true;
                int size = horizontalCalculator.size();

                if (size >= minimumCollection) {
                    this.horizontalStorage.put(size, horizontalCalculator.toArray(new Float[0]));
                }
            }
        }

        // Clear calculators for future use
        if (clear) {
            this.verticalCalculator.clear();
            this.horizontalCalculator.clear();
        }
    }

    // Separator

    public Collection<Float[]> getVerticalStorage() {
        return verticalStorage.values();
    }

    public Collection<Float[]> getHorizontalStorage() {
        return horizontalStorage.values();
    }
}
