package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.Piston;
import com.vagdedes.spartan.listeners.bukkit.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Collection;

public class Event_World implements Listener {

    private static final boolean v1_21 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_21);

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockBreak(BlockBreakEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), true);
        SpartanBlock b = new SpartanBlock(nb);
        boolean cancelled = e.isCancelled();
        p.movement.judgeGround();

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
            p.getExecutor(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastBreak).handle(cancelled, e);
            p.getExecutor(Enums.HackType.GhostHand).handle(cancelled, b);
        }
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, e);

        // Detections
        AntiCheatLogs.logMining(p, b, cancelled);

        if (p.getViolations(Enums.HackType.NoSwing).prevent()
                || p.getViolations(Enums.HackType.BlockReach).prevent()
                || p.getViolations(Enums.HackType.FastBreak).prevent()
                || p.getViolations(Enums.HackType.GhostHand).prevent()
                || p.getViolations(Enums.HackType.XRay).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockPlace(BlockPlaceEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), true);
        p.movement.judgeGround();

        if (p.getWorld() != nb.getWorld()) {
            return;
        }
        boolean cancelled = e.isCancelled();

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getExecutor(Enums.HackType.ImpossibleActions).handle(cancelled, e);
            p.getExecutor(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastPlace).handle(cancelled, e);
        }

        if (p.getViolations(Enums.HackType.FastPlace).prevent()
                || p.getViolations(Enums.HackType.BlockReach).prevent()
                || p.getViolations(Enums.HackType.ImpossibleActions).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Sign(SignChangeEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

        if (p.getViolations(Enums.HackType.Exploits).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Interact(PlayerInteractEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        if (PlayerLimitPerIP.isLimited(n)) {
            e.setCancelled(true);
        } else {
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;
            Block nb = e.getClickedBlock();
            Action action = e.getAction();
            boolean notNull = nb != null,
                    customBlock = notNull && ItemsAdder.is(nb);

            // Object
            p.calculateClicks(
                    action == Action.LEFT_CLICK_AIR
                            && (p.getLastDamageDealt().timePassed() <= 1_000
                            || p.getLastDamageReceived().timePassed() <= 1_000)
                            && !p.getNearbyEntities(
                            CombatUtils.maxHitDistance,
                            CombatUtils.maxHitDistance,
                            CombatUtils.maxHitDistance
                    ).isEmpty()
            );

            if (notNull) {
                // Detections
                if (!customBlock) {
                    p.getExecutor(Enums.HackType.BlockReach).handle(false, e);
                    p.getExecutor(Enums.HackType.FastBreak).handle(false, e);
                    p.getExecutor(Enums.HackType.ImpossibleActions).handle(false, e);
                }
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);

                if (!customBlock) {
                    p.getExecutor(Enums.HackType.GhostHand).handle(false, e);
                }
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);
            }
            // Detections
            if (!customBlock) {
                p.getExecutor(Enums.HackType.NoSwing).handle(false, e);
            }
            p.getExecutor(Enums.HackType.FastBow).handle(false, e);

            if (p.getViolations(Enums.HackType.GhostHand).prevent()
                    || p.getViolations(Enums.HackType.FastClicks).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Piston(BlockPistonExtendEvent e) {
        // Handlers
        Piston.run(e.getBlock(), e.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityExplosion(EntityExplodeEvent e) {
        if (v1_21 && SpartanBukkit.getPlayerCount() > 0) {
            Location location = e.getLocation();
            Collection<Entity> entities = location.getNearbyEntities(
                    CombatUtils.maxHitDistance,
                    CombatUtils.maxHitDistance,
                    CombatUtils.maxHitDistance
            );

            if (!entities.isEmpty()) {
                for (Entity entity : entities) {
                    if (entity instanceof Player) {
                        SpartanBukkit.getProtocol((Player) entity).spartanPlayer.trackers.add(
                                Trackers.TrackerType.ABSTRACT_VELOCITY,
                                AlgebraUtils.integerCeil(
                                        CombatUtils.maxHitDistance - entity.getLocation().distance(location)
                                ) * 5
                        );
                    }
                }
            }
        }
    }

}
