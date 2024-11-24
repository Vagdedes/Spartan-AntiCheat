package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.event.PlayerUseEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_Damaged;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class Event_Combat implements Listener {

    private static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.KillAura,
            Enums.HackType.HitReach,
            Enums.HackType.NoSwing,
            Enums.HackType.Criticals,
            Enums.HackType.FastClicks
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(EntityDamageByEntityEvent e) {
        event(e, false);
    }

    public static void event(EntityDamageByEntityEvent e, boolean packets) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean damagerIsPlayer = damager instanceof Player,
                entityIsPlayer = entity instanceof Player,
                entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
        if (damagerIsPlayer) {
            Player player = (Player) damager;
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.packetsEnabled() == packets) {
                // Detections
                if (entityAttack) {
                    protocol.spartan.calculateClicks(true);

                    if (entityIsPlayer || entity instanceof LivingEntity) {
                        boolean cancelled = e.isCancelled();
                        PlayerAttackEvent event = new PlayerAttackEvent(
                                player,
                                (LivingEntity) entity,
                                cancelled
                        );
                        protocol.spartan.getRunner(Enums.HackType.NoSwing).handle(cancelled, event);
                        protocol.spartan.getRunner(Enums.HackType.KillAura).handle(cancelled, event);
                        protocol.spartan.getRunner(Enums.HackType.Exploits).handle(cancelled, event);
                        if (!protocol.packetsEnabled())
                            protocol.spartan.getRunner(Enums.HackType.HitReach).handle(cancelled, event);
                        for (Enums.HackType hackType : handledChecks) {
                            if (protocol.spartan.getRunner(hackType).prevent()) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }

        if (entityIsPlayer) {
            Player player = (Player) entity;
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.packetsEnabled() == packets) {
                // Objects
                protocol.spartan.handleReceivedDamage();

                // Detections
                if (entityAttack && (damagerIsPlayer || damager instanceof LivingEntity)) {
                    boolean cancelled = e.isCancelled();
                    EntityAttackPlayerEvent event = new EntityAttackPlayerEvent(
                            player,
                            (LivingEntity) damager,
                            cancelled
                    );
                    protocol.spartan.getRunner(Enums.HackType.KillAura).handle(cancelled, event);
                }
            }
        } else {
            Event_Damaged.handlePassengers(entity, packets, e);
        }
    }

    public static void use(PlayerUseEvent e) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());
        PlayerAttackEvent attackEvent = new PlayerAttackEvent(e.getPlayer(), e.getTarget(), false);
        protocol.spartan.getRunner(Enums.HackType.HitReach).handle(false, attackEvent);
    }

}
