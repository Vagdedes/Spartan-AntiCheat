package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
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
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;
        Block nb = e.getBlock();
        Event_Chunks.cache(nb.getChunk(), false);
        p.movement.judgeGround();

        if (p.getWorld() != nb.getWorld()) {
            return;
        }
        boolean cancelled = e.isCancelled();

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getRunner(Enums.HackType.ImpossibleActions).handle(cancelled, e);
            p.getRunner(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getRunner(Enums.HackType.FastPlace).handle(cancelled, e);
        }

        if (p.getRunner(Enums.HackType.FastPlace).prevent()
                || p.getRunner(Enums.HackType.BlockReach).prevent()
                || p.getRunner(Enums.HackType.ImpossibleActions).prevent()) {
            e.setCancelled(true);
        }
    }

}
