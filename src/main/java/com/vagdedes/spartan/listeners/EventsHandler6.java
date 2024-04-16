package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class EventsHandler6 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Command(PlayerCommandPreprocessEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        String msg = e.getMessage();

        if (ChatProtection.runCommand(p, msg, false)) {
            e.setCancelled(true);
        } else {
            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).handle(e.isCancelled(), msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void KickEvent(PlayerKickEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // System
        if (Config.settings.getBoolean("Important.violations_reset_on_kick")) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.getViolations(hackType).reset();
            }
        }
    }

    // Separator

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
                    LivingEntity entity = (LivingEntity) defaultEntity;

                    if (!cancelled) {
                        // Compatibility
                        NoHitDelay.runDealDamage(p, entity);
                    }
                    if (!p.uuid.equals(defaultEntity.getUniqueId())
                            && !mcMMO.hasGeneralAbility(p)
                            && !MythicMobs.is(defaultEntity)
                            && !ItemsAdder.is(defaultEntity)) {
                        double[] utility = CombatUtils.get_X_Y_Distance(p, entity);

                        // Detections
                        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, new Object[]{e.getDamage(), entity});

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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void InventoryInteract(InventoryInteractEvent e) {
        HumanEntity he = e.getWhoClicked();

        if (he instanceof Player) {
            Player n = (Player) he;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.setInventory(n.getInventory(), n.getOpenInventory());
        }
    }
}
