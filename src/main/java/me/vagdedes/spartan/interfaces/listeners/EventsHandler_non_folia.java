package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.handlers.stability.Chunks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class EventsHandler_non_folia implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunks.load(e.getWorld(), e.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunks.unload(e.getWorld(), e.getChunk());
    }
}
