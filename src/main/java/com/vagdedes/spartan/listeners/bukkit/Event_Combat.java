package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.Shared;
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

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.KillAura,
            Enums.HackType.HitReach,
            Enums.HackType.NoSwing,
            Enums.HackType.Criticals
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean entityIsPlayer = entity instanceof Player;

        if (damager instanceof Player) {
            Player n = (Player) damager;

            if (!ProtocolLib.isTemporary(n)) {
                boolean entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
                SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                if (p != null) {
                    // Object
                    p.addDealtDamage(e);

                    // Detections
                    if (entityAttack) {
                        p.calculateClicks(true);

                        if (entityIsPlayer || entity instanceof LivingEntity) {
                            for (Enums.HackType hackType : handledChecks) {
                                if (p.getExecutor(hackType).prevent()) {
                                    e.setCancelled(true);
                                }
                            }
                            if (p.getExecutor(Enums.HackType.FastClicks).prevent()) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
                if (entityAttack
                        && !SpartanBukkit.packetsEnabled()
                        && (entityIsPlayer || entity instanceof LivingEntity)) {
                    Player player = (Player) damager;

                    if (!ProtocolLib.isTemporary(player)) {
                        Shared.attack(new PlayerAttackEvent(
                                player,
                                (LivingEntity) entity,
                                e.isCancelled()
                        ));
                    }
                }
            }
        }

        if (entityIsPlayer) {
            Player n = (Player) entity;

            if (!ProtocolLib.isTemporary(n)) {
                SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                if (p == null) {
                    return;
                }
                boolean cancelled = e.isCancelled();

                // Objects
                p.addReceivedDamage(e);

                // Detections
                p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
            }
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;

                        if (!ProtocolLib.isTemporary(n)) {
                            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                            if (p != null) {
                                // Objects
                                p.addReceivedDamage(e);
                            }
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

            if (!ProtocolLib.isTemporary(n)) {
                SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                if (p == null) {
                    return;
                }
                // Objects
                p.addReceivedDamage(e);
            }
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;

                        if (!ProtocolLib.isTemporary(n)) {
                            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                            if (p != null) {
                                // Objects
                                p.addReceivedDamage(e);
                            }
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
            Player n = (Player) entity;

            if (!ProtocolLib.isTemporary(n)) {
                SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                if (p == null) {
                    return;
                }
                // Detections
                p.getExecutor(Enums.HackType.FastBow).handle(e.isCancelled(), e);

                if (p.getExecutor(Enums.HackType.FastBow).prevent()) {
                    e.setCancelled(true);
                }
            }
        }
    }

}
