package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.combat.FastBow;
import me.vagdedes.spartan.checks.movement.NoSlowdown;
import me.vagdedes.spartan.checks.player.NoSwing;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.Teleport;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.FishingHook;
import me.vagdedes.spartan.handlers.identifiers.simple.GameModeProtection;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EventsHandler2 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        SpartanLocation to = new SpartanLocation(p, nto);

        // Object
        p.resetLocationData();

        // System
        Cache.clear(p, n,
                false,
                to.distance(e.getFrom()) >= MoveUtils.chunk && !Moderation.isDetectedAndPrevented(p),
                !Moderation.wasDetected(p),
                to);
        CombatProcessing.runTeleport(p);

        // Protections
        Teleport.run(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ItemChange(PlayerItemHeldEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Objects
        p.setInventory(n.getInventory(), n.getOpenInventory());

        // Detections
        NoSlowdown.runItemChange(p);
        NoSwing.runItemChange(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BowShot(EntityShootBowEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) e.getEntity());

            if (p == null) {
                return;
            }
            float force = e.getForce();
            Entity projectile = e.getProjectile();

            // Detections
            if (NoSlowdown.runBow(p)) {
                e.setCancelled(true);
            } else {
                // Detections
                FastBow.run(p, force);

                // Protections
                Damage.runBow(entity, projectile);

                if (HackPrevention.canCancel(p, Enums.HackType.FastBow)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Fish(PlayerFishEvent e) {
        Entity entity = e.getCaught();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);
            SpartanPlayer t = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null || t == null) {
                return;
            }
            // Protections
            p.getHandlers().disable(Handlers.HandlerType.Velocity, 10);
            FishingHook.run(p, t);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Gamemode(PlayerGameModeChangeEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        if (e.isCancelled()) {
            p.setGameMode(n.getGameMode());
        } else {
            GameMode old = p.getGameMode();

            if (old != null) {
                // Protections
                if (old != GameMode.SURVIVAL && old != GameMode.ADVENTURE) {
                    GameMode current = e.getNewGameMode();

                    if (current == GameMode.CREATIVE || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
                        GameModeProtection.run(p);
                    }
                }

                // Objects
                p.setGameMode(e.getNewGameMode());
            }
        }

        // Objects
        p.setFlying(n.isFlying(), n.getAllowFlight());
    }
}
