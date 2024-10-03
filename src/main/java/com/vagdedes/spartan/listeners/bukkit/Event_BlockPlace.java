package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class Event_BlockPlace implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(BlockPlaceEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), false);
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

        if (p.getExecutor(Enums.HackType.FastPlace).prevent()
                || p.getExecutor(Enums.HackType.BlockReach).prevent()
                || p.getExecutor(Enums.HackType.ImpossibleActions).prevent()) {
            e.setCancelled(true);
        }
    }

}
