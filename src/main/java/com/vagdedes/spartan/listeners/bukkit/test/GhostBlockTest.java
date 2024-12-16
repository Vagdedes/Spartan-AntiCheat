package com.vagdedes.spartan.listeners.bukkit.test;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class GhostBlockTest implements Listener {
    @EventHandler
    public void block(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void breAk(BlockBreakEvent event) {
        event.setCancelled(true);
    }
}
