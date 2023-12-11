package me.vagdedes.spartan.handlers.tracking;

import me.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.entity.Entity;

public class MovementProcessing {

    public static final double
            sprintingMaxSpeed = 0.287,
            walkingMaxSpeed = 0.2203;
    private static final int ticks = 2;

    public static void run(SpartanPlayer p, SpartanLocation to, Entity vehicle,
                           Buffer buffer, Cooldowns cooldowns, Decimals decimals,
                           double dis, double hor, double ver, double box, boolean groundGliding) {
        if (p.canRunChecks(false)) {
            // Damage
            Damage.runMove(p);

            // Vehicle Ticks Reset
            if (hor <= sprintingMaxSpeed && vehicle == null && p.isAnyOnGround()) {
                resetVehicleTicks(p);
            }

            // NMS Distance Caching
            p.setNmsDistance(dis, hor, ver, box);

            if (!groundGliding) {
                // Jump/Fall Identifier
                if (Math.abs(MoveUtils.jumping[1] - ver) < MoveUtils.getJumpingPrecision(p)) { // Last Jump
                    p.setLastJump();
                } else if (p.isFalling(ver)) { // Last Fall
                    p.setLastFall();
                }

                // Sneaking Counter
                if (p.isSneaking()) {
                    cooldowns.add("move-utils=sneaking-count", 10);
                } else {
                    String key = "move-utils=sneaking-count";
                    int counter = cooldowns.get(key);

                    if (counter > 0 && counter < 10) {
                        cooldowns.remove(key);
                        buffer.start(key, 300);
                    }
                }

                // Sprint/Walk Identifier
                if (dis > 0.1) {
                    boolean liquid = p.isSwimming() || Liquid.isLocation(p, to);

                    if (liquid // Liquid
                            || !p.getActivePotionEffects().isEmpty() // Potions
                            || p.isUsingItem() // Inventory
                            || PlayerData.getWalkSpeedDifference(p) > 0.0f) { // Server
                        p.setWalking(0);
                        p.setSprinting(0);
                    } else {
                        boolean walking = dis >= 0.215 && dis < walkingMaxSpeed,
                                sprinting = !walking && dis > 0.28 && dis < sprintingMaxSpeed,
                                ground = p.isAnyOnGround(),
                                nativeSprinting = p.isSprinting(),
                                walkJumping = !walking && !nativeSprinting && dis > 0.24 && dis < 0.29,
                                sprintJumping = nativeSprinting || !walking && !sprinting && dis > 0.5 && dis < 0.68;

                        if (ground && (walking || sprinting) && !BlockUtils.isSolid(to)) {
                            p.setWalking(walking ? ticks : 0);
                            p.setSprinting(sprinting ? ticks : 0);
                        } else {
                            p.setWalking(0);
                            p.setSprinting(0);
                        }

                        // Separator

                        if ((walkJumping || sprintJumping)
                                && p.isJumping(box)
                                && (ground
                                || PlayerData.isOnGround(p, to, 0, -1, 0)
                                || PlayerData.isOnGround(p, to, 0, -1.5, 0))) {
                            if (walkJumping) {
                                p.setJumpWalking(ticks);
                            }
                            if (sprintJumping) {
                                p.setJumpSprinting(ticks);
                            }
                        } else {
                            p.setJumpWalking(0);
                            p.setJumpSprinting(0);
                        }
                    }
                }
            }

            // Extra Packets
            String key = "player-data=extra-packets";

            if (!Moderation.wasDetected(p)) {
                int maxTicks = 20;
                double difference = p.getCustomDistance() - dis;
                decimals.add(key, difference, maxTicks);

                if (buffer.increase(key, 1) >= maxTicks) {
                    buffer.remove(key);

                    if (decimals.get(key, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                        p.setExtraPackets(p.getExtraPackets() + 1);
                    } else {
                        p.setExtraPackets(0);
                    }
                }
            } else { // Reset so it can again start from zero
                buffer.remove(key);
                decimals.remove(key);
            }
        }
    }

    public static void resetVehicleTicks(SpartanPlayer p) {
        p.getCooldowns().clear("player-data=vehicle");
    }
}
