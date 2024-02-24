package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.tracking.CombatProcessing;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
        boolean cancelled = e.isCancelled();
        SpartanLocation to = new SpartanLocation(p, nto);

        // Object
        if (!cancelled) {
            p.resetLocationData();
        }

        // System
        boolean distance = to.distance(e.getFrom()) >= MoveUtils.chunk && !p.wasDetected(true);
        Cache.clear(p, n,
                false,
                distance,
                distance,
                cancelled,
                to);

        if (!cancelled) {
            CombatProcessing.runTeleport(p);
        }
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
        p.getExecutor(Enums.HackType.NoSlowdown).handle(cancelled, e);
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
            boolean cancelled = e.isCancelled();
            Entity projectile = e.getProjectile();

            // Detections
            if (p.getExecutor(Enums.HackType.NoSlowdown).handle(cancelled, e)) {
                e.setCancelled(true);
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastBow).handle(cancelled, e);

                // Protections
                if (!cancelled) {
                    Damage.runBow(entity, projectile);
                }

                if (p.getViolations(Enums.HackType.FastBow).process()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Fish(PlayerFishEvent e) {
        Entity entity = e.getCaught();
        SpartanPlayer t = SpartanBukkit.getPlayer(e.getPlayer());

        if (t == null) {
            return;
        }
        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return;
            }

            // Protections
            Damage.addCooldown(p, p.equals(t) ? Damage.selfHitKey : Damage.fishingHookKey, 30);
        } else if (entity instanceof LivingEntity) {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? entity.getPassengers().toArray(new Entity[0]) : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            // Protections
                            Damage.addCooldown(p, p.equals(t) ? Damage.selfHitKey : Damage.fishingHookKey, 30);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Gamemode(PlayerGameModeChangeEvent e) {
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
        p.setFlying(n.isFlying());
    }
}
