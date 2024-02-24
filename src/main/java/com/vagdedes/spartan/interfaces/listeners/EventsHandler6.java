package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.compatibility.manual.essential.Essentials;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.protections.Explosion;
import com.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

public class EventsHandler6 implements Listener {

    private static final String cooldownKey = "combat=multiple-hits";

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Command(PlayerCommandPreprocessEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        String msg = e.getMessage();

        if (ChatProtection.runCommand(p, msg, false)) {
            e.setCancelled(true);
        } else {
            boolean cancelled = e.isCancelled();

            if (!cancelled) {
                // Compatibility
                Essentials.run(p, msg);
            }

            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).handle(cancelled, msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void KickEvent(PlayerKickEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // System
        if (!TestServer.isIdentified() && Config.settings.getBoolean("Important.violations_reset_on_kick")) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.getViolations(hackType).reset();
            }
        }
    }

    // Separator

    public static void runDealDamage(Event event,
                                     SpartanPlayer player,
                                     Entity defaultEntity,
                                     double damage,
                                     EntityDamageEvent.DamageCause dmg,
                                     boolean cancelled) {
        if (dmg != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || !(defaultEntity instanceof LivingEntity)
                || mcMMO.hasAbility(player)
                || MythicMobs.is(defaultEntity)
                || ItemsAdder.is(defaultEntity)) {
            return;
        }
        UUID playerUUID = player.getUniqueId(),
                entityUUID = defaultEntity.getUniqueId();

        if (playerUUID.equals(entityUUID)) {
            return;
        }
        LivingEntity entity = (LivingEntity) defaultEntity;
        Cooldowns cooldowns = player.getCooldowns();
        double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

        if (utility != null) {
            // Object
            player.calculateClickData(null, true);

            if (!cancelled) {
                // Compatibility
                NoHitDelay.runDealDamage(player, entity);

                // Handlers
                Damage.runDealtDamage(player);
            }

            // Detections
            player.getExecutor(Enums.HackType.Velocity).handle(cancelled, event);
            player.getExecutor(Enums.HackType.NoSwing).handle(cancelled, event);
            player.getExecutor(Enums.HackType.Criticals).handle(cancelled, new Object[]{damage, entity});

            if (cooldowns.canDo(cooldownKey)
                    || CombatUtils.isNewPvPMechanic(player, entity)) { // Multiple Hit Cooldown
                Object[] objects = new Object[]{entity, utility};
                player.getExecutor(Enums.HackType.KillAura).run(cancelled);
                player.getExecutor(Enums.HackType.KillAura).handle(cancelled, objects);
                player.getExecutor(Enums.HackType.HitReach).handle(cancelled, objects);
            }

            if (!cancelled) {
                // Object (Always after detections so to not refresh last-hit, last-damage, e.t.c.)
                if (entity instanceof Player) {
                    SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                    if (target != null) {
                        player.getProfile().getCombat().getFight(target).update(player);
                    }
                }
            }
        }
        cooldowns.add(cooldownKey, 3);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void DealDamage(EntityDamageByEntityEvent e) {
        if (!Compatibility.CompatibilityType.SmashHit.isFunctional()) {
            Entity en = e.getDamager();

            if (en instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) en);

                if (p == null) {
                    return;
                }
                double distance = MoveUtils.chunk / 2;
                Double nmsDistance = p.getNmsDistance();

                if (nmsDistance != null && nmsDistance <= distance
                        || p.getCustomDistance() <= distance) {
                    // Detections
                    runDealDamage(e, p, e.getEntity(), e.getDamage(), e.getCause(), e.isCancelled());
                }

                if (p.getViolations(Enums.HackType.KillAura).process()
                        || p.getViolations(Enums.HackType.Criticals).process()
                        || p.getViolations(Enums.HackType.NoSwing).process()
                        || p.getViolations(Enums.HackType.FastClicks).process()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    // Separator

    public static void runReceiveDamage(Entity damager, Entity entity, EntityDamageEvent.DamageCause dmg, boolean cancelled) {
        if (!damager.equals(entity)) {
            if (entity instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                if (p == null) {
                    return;
                }
                GameMode gameMode = p.getGameMode();

                if (cancelled && (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE)) {
                    return;
                }
                // Handlers
                Damage.runReceiveDamage(dmg, p, damager);
                Explosion.runDamage(p, damager, dmg);
                FloorProtection.runReceiveDamage(p, damager, dmg);

                if (MythicMobs.is(damager) || ItemsAdder.is(damager)) {
                    Damage.addCooldown(p, 40);
                }
            } else {
                Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? entity.getPassengers().toArray(new Entity[0]) : new Entity[]{entity.getPassenger()};

                if (passengers.length > 0) {
                    for (Entity passenger : passengers) {
                        if (passenger instanceof Player) {
                            SpartanPlayer p = SpartanBukkit.getPlayer((Player) passenger);

                            if (p != null) {
                                // Handlers
                                Damage.runReceiveDamage(dmg, p, damager);
                                Explosion.runDamage(p, damager, dmg);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ReceiveDamage(EntityDamageByEntityEvent e) {
        if (!Compatibility.CompatibilityType.SmashHit.isFunctional()) {
            Entity entity = e.getEntity();
            Entity damager = e.getDamager();

            if (damager != null && entity != null) {
                boolean cancelled = e.isCancelled();
                EntityDamageEvent.DamageCause cause = e.getCause();

                // Protections
                if (Damage.runDealAndReceiveDamage(damager, entity, cause, cancelled)) {
                    e.setCancelled(true);
                } else {
                    runReceiveDamage(damager, entity, cause, cancelled);
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
        PlayerAnimationType animationType = e.getAnimationType();

        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);

        // Object
        if (animationType == PlayerAnimationType.ARM_SWING) {
            p.calculateClickData(null, false);
        }
    }
}
