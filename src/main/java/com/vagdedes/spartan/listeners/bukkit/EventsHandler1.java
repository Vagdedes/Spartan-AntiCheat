package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class EventsHandler1 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent e) {
        Player n = e.getPlayer();
        SpartanBukkit.removeRealPlayer(n);
        SpartanPlayer p = SpartanBukkit.removePlayer(n);

        if (p == null) {
            return;
        }
        // Features
        MaximumCheckedPlayers.remove(p);

        // Utils
        PlayerLimitPerIP.remove(p);

        // Features
        ChatProtection.remove(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        Player n = e.getEntity();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).run(false);
        p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        // Objects
        Player killer = n.getKiller();

        if (killer != null && killer.isOnline()) {
            SpartanPlayer p2 = SpartanBukkit.getPlayer(killer);

            if (p2 != null) {
                PlayerFight fight = p.getProfile().playerCombat.getFight(p2);

                if (fight != null) {
                    fight.setWinner(p2);
                }
            }
        }
        p.resetTrackers();
        p.movement.setDetectionLocation(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Objects
        p.movement.setDetectionLocation(true);

        // Protections
        p.resetTrackers();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity defaultEntity = e.getEntity();
        boolean entityIsPlayer = defaultEntity instanceof Player;

        if (damager instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) damager);

            if (p == null) {
                return;
            }
            EntityDamageEvent.DamageCause cause = e.getCause();

            // Object
            p.addDealtDamage(e);

            // Detections
            if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                p.calculateClicks(null, true);

                if (entityIsPlayer || defaultEntity instanceof LivingEntity) {
                    boolean cancelled = e.isCancelled();

                    if (!p.uuid.equals(defaultEntity.getUniqueId())
                            && !mcMMO.hasGeneralAbility(p)
                            && !MythicMobs.is(defaultEntity)
                            && !ItemsAdder.is(defaultEntity)) {
                        LivingEntity entity = (LivingEntity) defaultEntity;
                        double[] utility = CombatUtils.get_X_Y_Distance(p, entity);

                        // Detections
                        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);

                        if (utility != null) {
                            Object[] objects = new Object[]{entity, utility};
                            p.getExecutor(Enums.HackType.KillAura).run(cancelled);
                            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, objects);
                            p.getExecutor(Enums.HackType.HitReach).handle(cancelled, objects);
                        }
                        if (!cancelled) {
                            // Object (Always after detections so to not refresh last-hit, last-damage, e.t.c.)
                            if (entityIsPlayer) {
                                SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                                if (target != null) {
                                    p.getProfile().playerCombat.getFight(target).update(p);
                                }
                            }
                        }

                        if (p.getViolations(Enums.HackType.KillAura).prevent()
                                || p.getViolations(Enums.HackType.Criticals).prevent()
                                || p.getViolations(Enums.HackType.NoSwing).prevent()
                                || p.getViolations(Enums.HackType.FastClicks).prevent()) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }

        if (entityIsPlayer) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) defaultEntity);

            if (p == null) {
                return;
            }
            // Objects
            p.addReceivedDamage(e);

            // Detections

            p.getExecutor(Enums.HackType.Speed).handle(e.isCancelled(), e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? defaultEntity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{defaultEntity.getPassenger()};

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

}
