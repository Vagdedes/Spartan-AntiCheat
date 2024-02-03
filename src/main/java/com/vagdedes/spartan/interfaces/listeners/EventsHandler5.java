package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.moderation.Debug;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.protections.Building;
import com.vagdedes.spartan.functionality.protections.Explosion;
import com.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.handlers.identifiers.simple.SensitiveBlockBreak;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
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
                p.getExecutor(Enums.HackType.FastEat).handle(e);
                p.getExecutor(Enums.HackType.NoSlowdown).handle(e);
                p.getExecutor(Enums.HackType.FastHeal).handle(e);

                if (p.getViolations(Enums.HackType.FastEat).process()
                        || p.getViolations(Enums.HackType.NoSlowdown).process()) {
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
                p.getExecutor(Enums.HackType.NoSwing).handle(e);

                if (p.getViolations(Enums.HackType.FastHeal).process()) {
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
                p.getExecutor(Enums.HackType.NoSwing).handle(e);
                p.getExecutor(Enums.HackType.BlockReach).handle(e);
                p.getExecutor(Enums.HackType.FastBreak).handle(e);
                p.getExecutor(Enums.HackType.GhostHand).handle(e);
                p.getExecutor(Enums.HackType.ImpossibleActions).handle(e);
            }

            // Features
            if (Debug.canRun()) {
                Debug.inform(p, Enums.Debug.MISC, "type: breaking, "
                        + "block: " + b.getType().toString().toLowerCase().replace("_", "-") + ", "
                        + "distance: " + AlgebraUtils.cut(p.getLocation().distance(b.getLocation()), 5));
            }

            // Detections
            DetectionNotifications.runMining(p, b);

            if (p.getViolations(Enums.HackType.NoSwing).process()
                    || p.getViolations(Enums.HackType.BlockReach).process()
                    || p.getViolations(Enums.HackType.ImpossibleActions).process()
                    || p.getViolations(Enums.HackType.FastBreak).process()
                    || p.getViolations(Enums.HackType.GhostHand).process()
                    || p.getViolations(Enums.HackType.XRay).process()) {
                e.setCancelled(true);
            }
        }

        // Protections
        SensitiveBlockBreak.run(p, cancelled, b);

        // Detections
        p.getExecutor(Enums.HackType.MorePackets).handle(e);
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
        BouncingBlocks.judge(p, p.getLocation());

        if (!cancelled) {
            // Detections
            if (!ItemsAdder.is(nb)) {
                p.getExecutor(Enums.HackType.ImpossibleActions).handle(e);
                p.getExecutor(Enums.HackType.BlockReach).handle(e);
                p.getExecutor(Enums.HackType.FastPlace).handle(e);
            }

            // Feature
            if (Debug.canRun()) {
                Debug.inform(p, Enums.Debug.MISC, "type: placing, "
                        + "block: " + b.getType() + ", "
                        + "distance: " + AlgebraUtils.cut(p.getLocation().distance(b.getLocation()), 2) + ", "
                        + "block-against: " + BlockUtils.materialToString(ba.getType()) + ", "
                        + "block-face: " + blockFace);
            }

            if (p.getViolations(Enums.HackType.FastPlace).process()
                    || p.getViolations(Enums.HackType.BlockReach).process()
                    || p.getViolations(Enums.HackType.ImpossibleActions).process()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void BlockExplode(BlockExplodeEvent e) {
        Explosion.runExplosion(e.getBlock());
    }
}
