package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.movement.MorePackets;
import me.vagdedes.spartan.checks.movement.NoSlowdown;
import me.vagdedes.spartan.checks.player.FastEat;
import me.vagdedes.spartan.checks.player.FastHeal;
import me.vagdedes.spartan.checks.player.NoSwing;
import me.vagdedes.spartan.checks.world.*;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.features.moderation.Debug;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.features.protections.Explosion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.simple.BlockBreak;
import me.vagdedes.spartan.handlers.identifiers.simple.BlockPlace;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class EventsHandler5 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Food(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            if (e.isCancelled()) {
                p.setFoodLevel(n.getFoodLevel(), false);
            } else {
                int lvl = e.getFoodLevel();

                // Detections
                FastEat.runFood(p, lvl);
                NoSlowdown.runFood(p, lvl);
                FastHeal.runFood(p, lvl);

                if (HackPrevention.canCancel(p, new Enums.HackType[]{Enums.HackType.FastEat, Enums.HackType.NoSlowdown})) {
                    e.setCancelled(true);
                    p.setFoodLevel(n.getFoodLevel(), false);
                } else {
                    // Objects (Always after detections)
                    p.setFoodLevel(lvl, false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.setHealth(n.getHealth());

            if (!e.isCancelled()) {
                // Detections
                FastHeal.run(p, e.getRegainReason());

                if (HackPrevention.canCancel(p, Enums.HackType.FastHeal)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockBreak(BlockBreakEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Block nb = e.getBlock();
        SpartanBlock b = new SpartanBlock(p, nb);
        boolean cancelled = e.isCancelled();

        if (!cancelled) {
            // Detections
            if (!ItemsAdder.is(nb)) {
                NoSwing.runBreak(p, b);
                ImpossibleActions.runBreak(p, b);
                BlockReach.runBreak(p, b);
                FastBreak.run(p, b);
                GhostHand.runBreak(p, b, e.isCancelled());
            }

            // Features
            if (Debug.canRun()) {
                Debug.inform(p, Enums.Debug.MISC, "type: breaking, "
                        + "block: " + b.getType().toString().toLowerCase().replace("_", "-") + ", "
                        + "distance: " + AlgebraUtils.cut(p.getLocation().distance(b.getLocation()), 5));
            }

            // Detections
            DetectionNotifications.runMining(p, b);

            if (HackPrevention.canCancel(p, new Enums.HackType[]{Enums.HackType.NoSwing, Enums.HackType.BlockReach,
                    Enums.HackType.ImpossibleActions, Enums.HackType.FastBreak, Enums.HackType.GhostHand,
                    Enums.HackType.XRay})) {
                e.setCancelled(true);
            }
        }

        // Protections
        BlockBreak.run(p, cancelled, b);

        // Detections
        MorePackets.handleMining(p, b);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockPlace(BlockPlaceEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Block nb = e.getBlock();
        SpartanBlock b = new SpartanBlock(p, nb);

        if (n.getWorld() != b.getWorld()) {
            return;
        }
        Block rba = e.getBlockAgainst();
        BlockFace blockFace = nb.getFace(rba);
        SpartanBlock ba = new SpartanBlock(p, rba);
        boolean cancelled = e.isCancelled();

        // Protections
        Building.runPlace(p, b, blockFace, cancelled);
        BlockPlace.runPlace(p, cancelled);
        BouncingBlocks.judge(p, p.getLocation());

        if (!cancelled) {
            // Detections
            if (!ItemsAdder.is(nb)) {
                ImpossibleActions.runPlace(p, b, ba, blockFace);
                FastPlace.run(p, b, true, true, true);
                BlockReach.runPlace(p, b, ba);
            }

            // Feature
            if (Debug.canRun()) {
                Debug.inform(p, Enums.Debug.MISC, "type: placing, "
                        + "block: " + b.getType() + ", "
                        + "distance: " + AlgebraUtils.cut(p.getLocation().distance(b.getLocation()), 2) + ", "
                        + "block-against: " + BlockUtils.materialToString(ba.getType()) + ", "
                        + "block-face: " + blockFace);
            }

            if (HackPrevention.canCancel(p, new Enums.HackType[]{Enums.HackType.FastPlace, Enums.HackType.BlockReach,
                    Enums.HackType.ImpossibleActions})) {
                e.setCancelled(true);
            }
        } else if (!ItemsAdder.is(nb)) {
            FastPlace.run(p, b, false, false, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void BlockExplode(BlockExplodeEvent e) {
        Explosion.runExplosion(e.getBlock());
    }
}
