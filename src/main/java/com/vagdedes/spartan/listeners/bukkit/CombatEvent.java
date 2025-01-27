package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.event.PlayerUseEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.listeners.bukkit.standalone.DamageEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CombatEvent implements Listener {

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
            PlayerProtocol protocol = PluginBase.getProtocol(player, true);

            if (protocol.packetsEnabled() == packets) {
                // Detections
                if (entityAttack) {
                    if (entityIsPlayer || entity instanceof LivingEntity) {
                        boolean cancelled = e.isCancelled();
                        PlayerAttackEvent event = new PlayerAttackEvent(
                                player,
                                (LivingEntity) entity,
                                cancelled
                        );
                        protocol.profile().executeRunners(cancelled, event);

                        for (Enums.HackType hackType : handledChecks) {
                            if (protocol.profile().getRunner(hackType).prevent()) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }

        if (entityIsPlayer) {
            Player player = (Player) entity;
            PlayerProtocol protocol = PluginBase.getProtocol(player, true);

            if (protocol.packetsEnabled() == packets) {
                // Objects
                protocol.bukkitExtra.handleReceivedDamage();

                // Detections
                if (entityAttack && (damagerIsPlayer || damager instanceof LivingEntity)) {
                    boolean cancelled = e.isCancelled();
                    protocol.profile().executeRunners(
                            cancelled,
                            new EntityAttackPlayerEvent(
                                    player,
                                    (LivingEntity) damager,
                                    cancelled
                            )
                    );
                }
            }
        } else {
            DamageEvent.handlePassengers(entity, packets, e);
        }
    }

    public static void use(PlayerUseEvent e) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.player, true);
        protocol.profile().executeRunners(
                false,
                new PlayerAttackEvent(e.player, e.target, false)
        );
    }

}
