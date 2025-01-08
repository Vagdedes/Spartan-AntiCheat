package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BlockPlaceEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(org.bukkit.event.block.BlockPlaceEvent e) {
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
            protocol.profile().getRunner(Enums.HackType.ImpossibleActions).handle(
                    cancelled,
                    new Block[]{block, blockAgainst}
            );
            protocol.profile().getRunner(Enums.HackType.BlockReach).handle(
                    cancelled,
                    new Block[]{block, blockAgainst}
            );
            protocol.profile().getRunner(Enums.HackType.FastPlace).handle(cancelled, block);
        }
        return protocol.profile().getRunner(Enums.HackType.FastPlace).prevent()
                || protocol.profile().getRunner(Enums.HackType.BlockReach).prevent()
                || protocol.profile().getRunner(Enums.HackType.ImpossibleActions).prevent();
    }

}
