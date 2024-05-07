package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class EventsHandler2 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Object
        p.movement.resetAirTicks();
        p.movement.resetVanillaAirTicks();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ItemChange(PlayerItemHeldEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        boolean cancelled = e.isCancelled();

        // Objects
        if (!cancelled) {
            p.setInventory(n.getInventory(), n.getOpenInventory());
        }

        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BowShot(EntityShootBowEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) e.getEntity());

            if (p == null) {
                return;
            }
            // Detections
            p.getExecutor(Enums.HackType.FastBow).handle(e.isCancelled(), e);

            if (p.getViolations(Enums.HackType.FastBow).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void GameMode(PlayerGameModeChangeEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        if (!e.isCancelled()) {
            // Objects
            p.setGameMode(e.getNewGameMode());
        }

        // Objects
        p.movement.setFlying(n.isFlying());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void VehicleEnter(VehicleEnterEvent e) {
        Entity en = e.getEntered();

        if (en instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) en);

            if (p == null) {
                return;
            }
            if (p.getViolations(Enums.HackType.Speed).prevent()
                    || p.getViolations(Enums.HackType.IrregularMovements).prevent()) {
                e.setCancelled(true);
            }
        }
    }
}
