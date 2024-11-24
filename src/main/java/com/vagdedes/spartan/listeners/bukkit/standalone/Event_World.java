package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.Piston;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.SignChangeEvent;
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
                        worlds.add(protocol.spartan.getWorld());
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
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), false);
        boolean cancelled = e.isCancelled();
        protocol.spartan.movement.judgeGround();

        // Detections
        if (!ItemsAdder.is(nb)) {
            protocol.spartan.getRunner(Enums.HackType.NoSwing).handle(cancelled, e);
            protocol.spartan.getRunner(Enums.HackType.BlockReach).handle(cancelled, e);
            protocol.spartan.getRunner(Enums.HackType.FastBreak).handle(cancelled, e);
            protocol.spartan.getRunner(Enums.HackType.GhostHand).handle(cancelled, nb);
        }
        protocol.spartan.getRunner(Enums.HackType.Exploits).handle(cancelled, e);
        protocol.spartan.getRunner(Enums.HackType.FastClicks).handle(cancelled, null);
        AntiCheatLogs.logMining(protocol, nb, cancelled);

        if (protocol.spartan.getRunner(Enums.HackType.NoSwing).prevent()
                || protocol.spartan.getRunner(Enums.HackType.BlockReach).prevent()
                || protocol.spartan.getRunner(Enums.HackType.FastBreak).prevent()
                || protocol.spartan.getRunner(Enums.HackType.GhostHand).prevent()
                || protocol.spartan.getRunner(Enums.HackType.XRay).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Sign(SignChangeEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;

        // Detections
        p.getRunner(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

        if (p.getRunner(Enums.HackType.Exploits).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        // Detections
        protocol.spartan.getRunner(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Interact(PlayerInteractEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;
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
                p.getRunner(Enums.HackType.BlockReach).handle(false, e);
                p.getRunner(Enums.HackType.FastBreak).handle(false, e);
                p.getRunner(Enums.HackType.ImpossibleActions).handle(false, e);
            }
            p.getRunner(Enums.HackType.FastEat).handle(false, e);

            if (!customBlock) {
                p.getRunner(Enums.HackType.GhostHand).handle(false, e);
            }
        } else {
            // Detections
            p.getRunner(Enums.HackType.FastEat).handle(false, e);
        }
        // Detections
        if (!customBlock) {
            p.getRunner(Enums.HackType.NoSwing).handle(false, e);
        }
        p.getRunner(Enums.HackType.FastBow).handle(false, e);

        if (p.getRunner(Enums.HackType.GhostHand).prevent()
                || p.getRunner(Enums.HackType.FastClicks).prevent()) {
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

}
