package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EventHandler_Incompatible implements Listener {

    // This has been placed here due to some forks not offering this event
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntitySpawn(EntitySpawnEvent e) {
        SpartanPlayer.cacheEntity(e.getEntity());
    }
}
