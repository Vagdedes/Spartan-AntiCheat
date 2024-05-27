package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.chat.StaffChat;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
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
import org.bukkit.event.server.ServerCommandEvent;

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

            // Detections
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, msg);

            // Protections
            if (!cancelled && StaffChat.run(p, msg) // Features
                    || !cancelled && ChatProtection.runChat(p, msg)) { // Features
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.addReceivedDamage(e);

            // Detections
            p.getExecutor(Enums.HackType.NoFall).handle(e.isCancelled(), e.getCause());
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

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
    private void Sign(SignChangeEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

        if (p.getViolations(Enums.HackType.Exploits).prevent()) {
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
            boolean notNull = nb != null,
                    customBlock = notNull && ItemsAdder.is(nb);

            // Object
            p.calculateClicks(action, false);

            if (notNull) {
                // Detections
                if (!customBlock) {
                    p.getExecutor(Enums.HackType.BlockReach).handle(false, e);
                    p.getExecutor(Enums.HackType.FastBreak).handle(false, e);
                    p.getExecutor(Enums.HackType.ImpossibleActions).handle(false, e);
                }
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);

                if (!customBlock) {
                    p.getExecutor(Enums.HackType.GhostHand).handle(false, e);
                }
            } else {
                // Detections
                p.getExecutor(Enums.HackType.FastEat).handle(false, e);
            }
            // Detections
            if (!customBlock) {
                p.getExecutor(Enums.HackType.NoSwing).handle(false, e);
            }
            p.getExecutor(Enums.HackType.FastBow).handle(false, e);

            if (p.getViolations(Enums.HackType.GhostHand).prevent()
                    || p.getViolations(Enums.HackType.FastClicks).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Command(ServerCommandEvent e) {
        CommandSender s = e.getSender();
        String msg = e.getCommand();

        // Protections
        if (ChatProtection.runConsoleCommand(s, msg)) {
            e.setCancelled(true);
        }
    }

}
