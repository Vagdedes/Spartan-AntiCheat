package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.combat.VelocityCheck;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.compatibility.manual.essential.Essentials;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.chat.ChatProtection;
import me.vagdedes.spartan.functionality.commands.RawCommands;
import me.vagdedes.spartan.functionality.commands.UnbanCommand;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.moderation.Debug;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.protections.Explosion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
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

        if (ChatProtection.runCommand(p, msg, false)
                || RawCommands.run(p, msg)) {
            e.setCancelled(true);
        } else {
            // Compatibility
            Essentials.run(p, msg);
            UnbanCommand.run(p, msg);

            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).handle(msg);
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

            double distance = utility[2];
            SpartanLocation loc = player.getLocation();

            if (!cancelled) {
                SpartanPlayer targetPlayer = entity instanceof Player ? SpartanBukkit.getPlayer((Player) entity) : null;
                boolean isPlayer = targetPlayer != null;

                // Compatibility
                NoHitDelay.runDealDamage(player, entity);

                // Feature
                if (Debug.canRun()) {
                    Debug.inform(player, Enums.Debug.COMBAT, "entity: " + CombatUtils.entityToString(entity) + ", "
                            + "width: " + AlgebraUtils.cut(utility[0], 5) + ", "
                            + "height: " + AlgebraUtils.cut(utility[1], 5) + ", "
                            + "distance: " + AlgebraUtils.cut(distance, 2));
                }

                // Notification
                if ((!isPlayer || !targetPlayer.isMoving(true) || !PlayerData.isInActivePlayerCombat(targetPlayer)) && Permissions.isStaff(player)) {
                    String message = AwarenessNotifications.getOptionalNotification(
                            "It is recommended to fight a player that's also fighting you to properly test the combat checks. "
                                    + "Animals, mobs, alt-accounts & NPCs come with slower detections.");

                    if (message != null) {
                        if (AwarenessNotifications.canSend(playerUUID, "combat")) {
                            player.sendMessage(message);
                        }
                        if (targetPlayer != null && AwarenessNotifications.canSend(entityUUID, "combat")) {
                            targetPlayer.sendMessage(message);
                        }
                    }
                }

                // Object
                if (isPlayer) {
                    PlayerFight fight = player.getProfile().getCombat().getCurrentFight(targetPlayer);

                    if (fight != null) {
                        int entities = PlayerOpponent.getEntities(player);
                        PlayerOpponent[] opponents = fight.getOpponent(player.getName());
                        opponents[0].increaseHits(player, distance, entities);
                        opponents[1].updateData(targetPlayer, entities);
                    }
                }

                // Detections
                player.getExecutor(Enums.HackType.NoSwing).handle(event);
                player.getExecutor(Enums.HackType.Criticals).handle(new Object[]{damage, entity});
                runDealDamageChild(player, cooldowns, entity, utility, false);

                // Handlers
                Damage.runDealtDamage(player);
            } else {
                runDealDamageChild(player, cooldowns, entity, utility, true);
            }
        }
        cooldowns.add(cooldownKey, 3);
    }

    private static void runDealDamageChild(SpartanPlayer player, Cooldowns cooldowns,
                                           LivingEntity entity,
                                           double[] utility,
                                           boolean cancelled) {
        if (!cancelled
                || TestServer.isIdentified()
                || Config.settings.getBoolean("Detections.allow_cancelled_hit_checking")
                || PlayerData.isInActivePlayerCombat(player)) {
            boolean hasMultipleHitCooldown = !cooldowns.canDo(cooldownKey) && CombatUtils.isNewPvPMechanic(player, entity);

            // Detections
            if (!hasMultipleHitCooldown) {
                Object[] objects = new Object[]{entity, utility};
                player.getExecutor(Enums.HackType.HitReach).handle(objects);
                player.getExecutor(Enums.HackType.KillAura).handle(objects);
            }
        }
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

                if (p.getNmsDistance() <= distance || p.getCustomDistance() <= distance) {
                    // Detections
                    runDealDamage(e, p, e.getEntity(), e.getDamage(), e.getCause(), e.isCancelled());
                }

                if (p.getViolations(Enums.HackType.KillAura).process()
                        || p.getViolations(Enums.HackType.Criticals).process()
                        || p.getViolations(Enums.HackType.NoSwing).process()
                        || p.getViolations(Enums.HackType.FastClicks).process()
                        || p.getViolations(Enums.HackType.Velocity).process()) {
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

                // Detections
                if (!(damager instanceof Player)) {
                    p.getExecutor(Enums.HackType.Velocity).handle(VelocityCheck.REFRESH);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Animation(PlayerAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        PlayerAnimationType animationType = e.getAnimationType();

        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e);

        // Object
        if (animationType == PlayerAnimationType.ARM_SWING) {
            p.calculateClickData(null, false);
        }
    }
}
