package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits;
import com.vagdedes.spartan.abstraction.check.implementation.inventory.ImpossibleInventory;
import com.vagdedes.spartan.abstraction.check.implementation.movement.MorePackets;
import com.vagdedes.spartan.abstraction.check.implementation.movement.NoFall;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.Speed;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
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
        boolean cancelled = e.isCancelled();

        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation vehicle = p.movement.getVehicleLocation();
        SpartanLocation
                to = vehicle != null ? vehicle : new SpartanLocation(p, nto),
                from = new SpartanLocation(p, e.getFrom());

        // Objects
        to.retrieveDataFrom(p.movement.getLocation());
        from.retrieveDataFrom(to);

        // Values
        double preXZ = AlgebraUtils.getSquare(to.getX(), from.getX()) + AlgebraUtils.getSquare(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getSquare(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);
        float fall = p.getFallDistance();

        if (!p.movement.processLastMoveEvent(to, from, ver)) {
            return;
        }
        MovementProcessing.run(n, p, to, dis, hor, ver, box, fall);

        // Patterns
        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).prevent()) {
                break;
            }
        }

        // Detections
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, Exploits.HEAD);
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, Exploits.MOVEMENT);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.KillAura).run(cancelled);

        if (dis > 0.0) {
            p.getExecutor(Enums.HackType.NoFall).run(cancelled);
            p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
            p.getExecutor(Enums.HackType.Speed).run(cancelled);
            p.getExecutor(Enums.HackType.MorePackets).run(cancelled);
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
    }
}
