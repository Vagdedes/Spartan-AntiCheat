package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.movement.NoFall;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.functionality.chat.ChatProtection;
import me.vagdedes.spartan.functionality.chat.StaffChat;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.moderation.BanManagement;
import me.vagdedes.spartan.functionality.protections.*;
import me.vagdedes.spartan.handlers.bug.FalseFallDamage;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class EventsHandler3 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Chat(AsyncPlayerChatEvent e) {
        Player n = e.getPlayer();

        if (PlayerLimitPerIP.isLimited(n)) {
            e.setCancelled(true);
        } else {
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            String msg = e.getMessage();

            // Protections
            if (p.getExecutor(Enums.HackType.Exploits).handle(msg) // Detections
                    || StaffChat.run(p, msg) // Features
                    || ChatProtection.runChat(p, msg)) { // Features
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        EntityDamageEvent.DamageCause dmg = e.getCause();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            GameMode gameMode = p.getGameMode();
            boolean cancelled = e.isCancelled()
                    && (gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE);
            double damage = e.getDamage();

            // Objects
            p.setFallDistance(n.getFallDistance(), false);
            p.setHealth(n.getHealth());
            p.setLastDamageCause(e, damage, n.getMaximumNoDamageTicks());

            if (cancelled) {
                // Detections
               p.getExecutor(Enums.HackType.NoFall).handle(e);
            } else {
                // Detections
                if (dmg == EntityDamageEvent.DamageCause.FALL) {
                    p.getExecutor(Enums.HackType.NoFall).handle(e);
                }

                // Handlers
                p.getHandlers().disable(Handlers.HandlerType.Velocity, 2);
                Damage.runReceivedDamage(p, dmg);
                Explosion.runDamage(p, null, dmg);

                // Detections
                ((NoFall) p.getExecutor(Enums.HackType.NoFall)).manageRatio(dmg, e.getDamage(), false);

                if (FalseFallDamage.runDamage(p, dmg)) {
                    e.setCancelled(true);
                } else {
                    // Protections
                    FloorProtection.runDamage(p, dmg);
                }
            }
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? entity.getPassengers().toArray(new Entity[0]) : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                double damage = e.getDamage();

                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            // Objects
                            p.setFallDistance(n.getFallDistance(), false);
                            p.setHealth(n.getHealth());
                            p.setLastDamageCause(e, damage, n.getMaximumNoDamageTicks());

                            // Handlers
                            Damage.runReceivedDamage(p, dmg);
                            Explosion.runDamage(p, null, dmg);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void Login(PlayerLoginEvent e) {
        Player n = e.getPlayer();
        UUID uuid = n.getUniqueId();

        // Protections
        CheckProtection.cancel(uuid, SpartanBukkit.hasResourcePack ? ((LagLeniencies.maxServerPing / 1000) * 20) : 5, false);

        // Configuration
        BanManagement.run(n, e);
        ReconnectCooldown.run(n, e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Sign(SignChangeEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(e.getLines());

        if (p.getViolations(Enums.HackType.Exploits).process()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Interact(PlayerInteractEvent e) {
        Player n = e.getPlayer();

        if (PlayerLimitPerIP.isLimited(n)) {
            e.setCancelled(true);
        } else {
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            Block nb = e.getClickedBlock();
            Action action = e.getAction();
            boolean cancelled = e.isCancelled(),
                    notNull = nb != null,
                    customBlock = notNull && ItemsAdder.is(nb);

            // Object
            p.calculateClickData(action, false);

            if (notNull) {
                SpartanBlock b = new SpartanBlock(p, nb);

                // Detections
                if (!customBlock) {
                    p.getExecutor(Enums.HackType.BlockReach).handle(e);
                    p.getExecutor(Enums.HackType.FastBreak).handle(e);
                    p.getExecutor(Enums.HackType.ImpossibleActions).handle(e);
                }
                p.getExecutor(Enums.HackType.FastEat).handle(e);
                p.getExecutor(Enums.HackType.NoSlowdown).handle(e);
                p.getExecutor(Enums.HackType.ItemDrops).handle(e);

                // Protections
                if (!cancelled) {
                    if (InteractionsPerTick.run(p, b, action)/* || FenceClick.run(p, b, action)*/) {
                        e.setCancelled(true);
                    } else if (!customBlock) {
                        // Detections
                        p.getExecutor(Enums.HackType.GhostHand).handle(e);
                        Liquid.runInteract(p, action);
                    }
                }
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastClicks).handle(e);
                p.getExecutor(Enums.HackType.FastEat).handle(e);
            }
            // Detections
            if (!customBlock) {
                p.getExecutor(Enums.HackType.NoSwing).handle(e);
            }
            p.getExecutor(Enums.HackType.FastBow).handle(e);

            if (p.getViolations(Enums.HackType.GhostHand).process()
                    || p.getViolations(Enums.HackType.FastClicks).process()) {
                e.setCancelled(true);
            }
        }
    }
}
