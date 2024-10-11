package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.Piston;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Event_World implements Listener {

    private static final boolean v1_21 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_21);
    private static final Map<World, Map<Long, List<Entity>>> entities = new LinkedHashMap<>();

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (SpartanBukkit.hasPlayerCount()) {
                if (MultiVersion.folia || SpartanBukkit.packetsEnabled()) {
                    Set<World> worlds = new HashSet<>();

                    for (SpartanProtocol protocol : SpartanBukkit.getProtocols()) {
                        worlds.add(protocol.spartanPlayer.getWorld());
                    }
                    for (World world : worlds) {
                        Map<Long, List<Entity>> perChunkEntities = new LinkedHashMap<>();

                        for (Entity entity : world.getEntities()) {
                            Location location = ProtocolLib.getLocation(entity);
                            long hash = Event_Chunks.hashCoordinates(
                                    SpartanLocation.getChunkPos(location.getBlockX()),
                                    SpartanLocation.getChunkPos(location.getBlockZ())
                            );
                            perChunkEntities.computeIfAbsent(
                                    hash,
                                    k -> new CopyOnWriteArrayList<>()
                            ).add(entity);
                        }
                        entities.put(world, perChunkEntities);
                    }
                    Iterator<World> iterator = entities.keySet().iterator();

                    while (iterator.hasNext()) {
                        if (!worlds.contains(iterator.next())) {
                            iterator.remove();
                        }
                    }
                } else {
                    entities.clear();
                }
            }
        }, 1L, 1L);
    }

    public static Map<Long, List<Entity>> getEntities(World world) {
        return entities.get(world);
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockBreak(BlockBreakEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), false);
        boolean cancelled = e.isCancelled();
        p.movement.judgeGround();

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
            p.getExecutor(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastBreak).handle(cancelled, e);
            p.getExecutor(Enums.HackType.GhostHand).handle(cancelled, nb);
        }
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, e);
        p.getExecutor(Enums.HackType.FastClicks).handle(cancelled, null);
        AntiCheatLogs.logMining(p, nb, cancelled);

        if (p.getExecutor(Enums.HackType.NoSwing).prevent()
                || p.getExecutor(Enums.HackType.BlockReach).prevent()
                || p.getExecutor(Enums.HackType.FastBreak).prevent()
                || p.getExecutor(Enums.HackType.GhostHand).prevent()
                || p.getExecutor(Enums.HackType.XRay).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Sign(SignChangeEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

        if (p.getExecutor(Enums.HackType.Exploits).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        // Detections
        protocol.spartanPlayer.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Interact(PlayerInteractEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        Block nb = e.getClickedBlock();
        Action action = e.getAction();
        boolean notNull = nb != null,
                customBlock = notNull && ItemsAdder.is(nb);

        // Object
        p.calculateClicks(
                action == Action.LEFT_CLICK_AIR
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

        if (p.getExecutor(Enums.HackType.GhostHand).prevent()
                || p.getExecutor(Enums.HackType.FastClicks).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Piston(BlockPistonExtendEvent e) {
        if (!e.isCancelled()) {
            // Handlers
            Piston.run(e.getBlock(), e.getBlocks());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void EntityExplosion(EntityExplodeEvent e) {
        if (v1_21
                && !e.isCancelled()
                && SpartanBukkit.getPlayerCount() > 0) {
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
                                PlayerTrackers.TrackerType.ABSTRACT_VELOCITY,
                                AlgebraUtils.integerCeil(
                                        CombatUtils.maxHitDistance
                                                - ProtocolLib.getLocation((Player) entity).distance(location)
                                ) * 5
                        );
                    }
                }
            }
        }
    }

}
