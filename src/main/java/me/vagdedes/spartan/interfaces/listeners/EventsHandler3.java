package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.combat.FastBow;
import me.vagdedes.spartan.checks.combat.fastClicks.FastClicks;
import me.vagdedes.spartan.checks.exploits.Chat;
import me.vagdedes.spartan.checks.exploits.SignLineLength;
import me.vagdedes.spartan.checks.inventory.ItemDrops;
import me.vagdedes.spartan.checks.movement.NoFall;
import me.vagdedes.spartan.checks.movement.NoSlowdown;
import me.vagdedes.spartan.checks.player.FastEat;
import me.vagdedes.spartan.checks.player.NoSwing;
import me.vagdedes.spartan.checks.world.BlockReach;
import me.vagdedes.spartan.checks.world.FastBreak;
import me.vagdedes.spartan.checks.world.GhostHand;
import me.vagdedes.spartan.checks.world.ImpossibleActions;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.features.chat.ChatProtection;
import me.vagdedes.spartan.features.chat.StaffChat;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.moderation.BanManagement;
import me.vagdedes.spartan.features.protections.*;
import me.vagdedes.spartan.handlers.bug.FalseFallDamage;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
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
            if (Chat.run(p, msg)  // Detections
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
                NoFall.manageRatio(p, dmg, damage, true);
            } else {
                // Detections
                if (dmg == EntityDamageEvent.DamageCause.FALL) {
                    NoFall.handleDamage(p);
                }

                // Handlers
                p.getHandlers().disable(Handlers.HandlerType.Velocity, 2);
                Damage.runReceivedDamage(p, dmg);
                Explosion.runDamage(p, null, dmg);

                // Detections
                NoFall.manageRatio(p, dmg, e.getDamage(), false);

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
        SignLineLength.run(p, e.getLines());

        if (HackPrevention.canCancel(p, Enums.HackType.Exploits)) {
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
                    BlockReach.runInteract(p, b);
                    ImpossibleActions.runInteract(p, action, b);
                    FastBreak.runInteract(p, action, b);
                }
                FastEat.runInteract(p, b, action);
                NoSlowdown.runCake(p, b, action);
                ItemDrops.runInteract(p, b, action);

                // Protections
                if (!cancelled) {
                    if (InteractionsPerTick.run(p, b, action)/* || FenceClick.run(p, b, action)*/) {
                        e.setCancelled(true);
                    } else if (!customBlock) {
                        // Detections
                        GhostHand.runInteract(p, b, action);
                        Liquid.runInteract(p, action);
                    }
                    Building.runInteract(p, b, action);
                }
            } else {
                // Detections
                FastClicks.run(p, action);
                FastEat.runInteract(p, null, action);
            }
            // Detections
            if (!customBlock) {
                NoSwing.runInteract(p, action);
            }
            FastBow.runInteract(p, action);

            if (HackPrevention.canCancel(p, new Enums.HackType[]{Enums.HackType.GhostHand, Enums.HackType.FastClicks})) {
                e.setCancelled(true);
            }
        }
    }
}
