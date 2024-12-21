package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class Event_BlockPlace implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(BlockPlaceEvent e) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer(), true);

        if (event(protocol, e.getBlock(), e.getBlockAgainst(), e.isCancelled())) {
            e.setCancelled(true);
        }
    }

    public static boolean event(
            SpartanProtocol protocol,
            Block block,
            Block blockAgainst,
            boolean cancelled
    ) {
        protocol.spartan.movement.judgeGround();

        if (protocol.getWorld() != block.getWorld()) {
            return false;
        }

        // Detections
        if (!ItemsAdder.is(block)) {
            protocol.spartan.getRunner(Enums.HackType.ImpossibleActions).handle(
                    cancelled,
                    new Block[]{block, blockAgainst}
            );
            protocol.spartan.getRunner(Enums.HackType.BlockReach).handle(
                    cancelled,
                    new Block[]{block, blockAgainst}
            );
            protocol.spartan.getRunner(Enums.HackType.FastPlace).handle(cancelled, block);
        }
        return protocol.spartan.getRunner(Enums.HackType.FastPlace).prevent()
                || protocol.spartan.getRunner(Enums.HackType.BlockReach).prevent()
                || protocol.spartan.getRunner(Enums.HackType.ImpossibleActions).prevent();
    }

}
