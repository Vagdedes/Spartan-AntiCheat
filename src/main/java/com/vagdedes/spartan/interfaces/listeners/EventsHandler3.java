package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.checks.movement.NoFall;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.chat.StaffChat;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.protections.Explosion;
import com.vagdedes.spartan.functionality.protections.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.protections.ReconnectCooldown;
import com.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import com.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Chat(AsyncPlayerChatEvent e) {
        Player n = e.getPlayer();

        if (PlayerLimitPerIP.isLimited(n)) {
            e.setCancelled(true);
        } else {
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();
            String msg = e.getMessage();

            // Protections
            if (p.getExecutor(Enums.HackType.Exploits).handle(cancelled, msg) // Detections
                    || !cancelled && StaffChat.run(p, msg) // Features
                    || !cancelled && ChatProtection.runChat(p, msg)) { // Features
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

            // Objects
            p.setFallDistance(n.getFallDistance(), false);
            p.setHealth(n.getHealth());
            p.setLastDamageCause(e, n.getMaximumNoDamageTicks());

            if (cancelled) {
                // Detections
                p.getExecutor(Enums.HackType.NoFall).handle(false, e);
            } else {
                // Detections
                if (dmg == EntityDamageEvent.DamageCause.FALL) {
                    p.getExecutor(Enums.HackType.NoFall).handle(false, e);
                }

                // Handlers
                p.getHandlers().disable(Handlers.HandlerType.Velocity, 2);
                Damage.runReceivedDamage(p, dmg);
                Explosion.runDamage(p, null, dmg);

                // Detections
                ((NoFall) p.getExecutor(Enums.HackType.NoFall)).manageRatio(dmg, e.getDamage(), false);

                // Protections
                FloorProtection.runDamage(p, dmg);
            }
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? entity.getPassengers().toArray(new Entity[0]) : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            // Objects
                            p.setFallDistance(n.getFallDistance(), false);
                            p.setHealth(n.getHealth());
                            p.setLastDamageCause(e, n.getMaximumNoDamageTicks());

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
        CheckProtection.cancel(uuid, SpartanBukkit.hasResourcePack ? 100 : 5);

        // Configuration
        ReconnectCooldown.run(n, e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Sign(SignChangeEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

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
                    p.getExecutor(Enums.HackType.BlockReach).handle(false, e);
                    p.getExecutor(Enums.HackType.FastBreak).handle(false, e);
                    p.getExecutor(Enums.HackType.ImpossibleActions).handle(false, e);
                }
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);
                p.getExecutor(Enums.HackType.NoSlowdown).handle(false, e);
                p.getExecutor(Enums.HackType.ItemDrops).handle(false, e);

                // Detections
                if (!customBlock) {
                    p.getExecutor(Enums.HackType.GhostHand).handle(false, e);
                    Liquid.runInteract(p, action);
                }
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastClicks).handle(false, e);
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);
            }
            // Detections
            if (!customBlock) {
                p.getExecutor(Enums.HackType.NoSwing).handle(false, e);
            }
            p.getExecutor(Enums.HackType.FastBow).handle(false, e);

            if (p.getViolations(Enums.HackType.GhostHand).process()
                    || p.getViolations(Enums.HackType.FastClicks).process()) {
                e.setCancelled(true);
            }
        }
    }
}
