package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits;
import com.vagdedes.spartan.abstraction.check.implementation.inventory.ImpossibleInventory;
import com.vagdedes.spartan.abstraction.check.implementation.movement.MorePackets;
import com.vagdedes.spartan.abstraction.check.implementation.movement.NoFall;
import com.vagdedes.spartan.abstraction.check.implementation.movement.NoSlowdown;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.Speed;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.identifiers.simple.CheckDelay;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.functionality.tracking.ServerFlying;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.java.HashUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class EventsHandler7 implements Listener {

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            NoFall.check,
            IrregularMovements.check,
            NoSlowdown.check,
            Speed.check,
            MorePackets.check,
            ImpossibleInventory.check,
            Exploits.check
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void PlayerFlight(PlayerToggleFlightEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Objects
        p.movement.setFlying(e.isCancelled() ? n.isFlying() : e.isFlying() || n.isFlying());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void TabCompletion(PlayerChatTabCompleteEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Protections
        if (ChatProtection.runCommand(p, e.getChatMessage(), true)) {
            e.getTabCompletions().clear();
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        PlayerUtils.update(p, n, false);
        boolean cancelled = e.isCancelled();

        if (!p.canDo(false)) {
            ServerFlying.run(p);
            return;
        }
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation to = p.movement.setEventLocation(nto),
                from = new SpartanLocation(p, e.getFrom());
        p.movement.processLastMoveEvent(to, from);

        // Objects
        to.retrieveDataFrom(p.movement.getLocation());
        from.retrieveDataFrom(to);

        if (to.getYaw() != from.getYaw() || to.getPitch() != from.getPitch()) {
            p.movement.setLastHeadMovement();
        }

        // Values
        double preXZ = AlgebraUtils.getPreDistance(to.getX(), from.getX()) + AlgebraUtils.getPreDistance(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getPreDistance(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);
        float fall = p.getFallDistance();

        // Detections (Always previous so it counts the previous tick and makes the current tick's prediction)
        int situation = HashUtils.extendInt(HashUtils.hashPlayer(p), HashUtils.hashEnvironment(p, to));

        // Patterns
        SpartanBukkit.movementPatterns.learn(p, situation, dis, hor, ver, fall);

        // Handlers
        MovementProcessing.run(p, dis, hor, ver, box, fall);
        CombatProcessing.runMove(p, to);

        // Detections
        p.getExecutor(Enums.HackType.IrregularMovements).handle(false, situation);
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, Exploits.HEAD);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.KillAura).run(cancelled);
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, dis);

        if (dis == 0.0) {
            // Handlers
            for (Enums.HackType hackType : handledChecks) {
                if (p.getViolations(hackType).prevent()) {
                    break;
                }
            }
            return;
        }

        // Detections
        ServerFlying.run(p);
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, Speed.SPRINT);
        p.getExecutor(Enums.HackType.NoFall).run(cancelled);
        p.getExecutor(Enums.HackType.NoSlowdown).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, Speed.WATER);
        p.getExecutor(Enums.HackType.MorePackets).run(cancelled);

        // Handlers
        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).prevent()) {
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WorldChange(PlayerChangedWorldEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Object
        p.resetHandlers();

        // Detections
        CheckDelay.cancel(p.uuid, 20);

        // System
        p.getExecutor(Enums.HackType.MorePackets).handle(false, p.movement.getLocation());
    }
}
