package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Shared;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Event_Combat implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean entityIsPlayer = entity instanceof Player;

        if (damager instanceof Player) {
            boolean entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) damager);

            if (p != null) {
                // Object
                p.addDealtDamage(e);

                // Detections
                if (entityAttack) {
                    p.calculateClicks(null, true);

                    if (entityIsPlayer || entity instanceof LivingEntity) {
                        if (entityIsPlayer && !e.isCancelled()) {
                            SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                            if (target != null) {
                                p.getProfile().playerCombat.getFight(target).update(p);
                            }
                        }

                        for (Enums.HackType hackType : Shared.handledCombatChecks) {
                            if (p.getViolations(hackType).prevent()) {
                                e.setCancelled(true);
                            }
                        }
                        if (p.getViolations(Enums.HackType.FastClicks).prevent()) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
            if (entityAttack
                    && (entityIsPlayer || entity instanceof LivingEntity)) {
                Player player = (Player) damager;

                if (!SpartanBukkit.packetsEnabled(player)) {
                    Shared.attack(new PlayerAttackEvent(
                            player,
                            (LivingEntity) entity,
                            e.isCancelled())
                    );
                }
            }
        }

        if (entityIsPlayer) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();

            // Objects
            p.addReceivedDamage(e);

            // Detections
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        SpartanPlayer p = SpartanBukkit.getPlayer((Player) passenger);

                        if (p != null) {
                            // Objects
                            p.addReceivedDamage(e);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.addReceivedDamage(e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            // Objects
                            p.addReceivedDamage(e);
                        }
                    }
                }
            }
        }
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

}
