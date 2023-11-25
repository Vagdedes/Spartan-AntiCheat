package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.protections.Teleport;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
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
        p.getExecutor(Enums.HackType.NoSlowdown).handle(e);
        p.getExecutor(Enums.HackType.NoSwing).handle(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BowShot(EntityShootBowEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) e.getEntity());

            if (p == null) {
                return;
            }
            Entity projectile = e.getProjectile();

            // Detections
            if (p.getExecutor(Enums.HackType.NoSlowdown).handle(e)) {
                e.setCancelled(true);
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastBow).handle(e);

                // Protections
                Damage.runBow(entity, projectile);

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
        p.setFlying(n.isFlying(), n.getAllowFlight());
    }
}
