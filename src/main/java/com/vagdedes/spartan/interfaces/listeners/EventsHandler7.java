package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.checks.exploits.Exploits;
import com.vagdedes.spartan.checks.inventory.ImpossibleInventory;
import com.vagdedes.spartan.checks.movement.MorePackets;
import com.vagdedes.spartan.checks.movement.NoFall;
import com.vagdedes.spartan.checks.movement.NoSlowdown;
import com.vagdedes.spartan.checks.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.checks.movement.speed.Speed;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.protections.ServerFlying;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.tracking.CombatProcessing;
import com.vagdedes.spartan.handlers.tracking.MovementProcessing;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
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

    private static boolean heavyMovementChecks = false;
    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            NoFall.check,
            IrregularMovements.check,
            NoSlowdown.check,
            Speed.check,
            MorePackets.check,
            ImpossibleInventory.check,
            Exploits.check
    };

    static {
        refresh();
    }

    public static void refresh() {
        heavyMovementChecks = false;

        for (Enums.HackType hackType :
                new Enums.HackType[]{
                        NoFall.check,
                        IrregularMovements.check,
                        NoSlowdown.check,
                        Speed.check,
                        MorePackets.check}) {
            if (hackType.getCheck().isEnabled(null, null, null)) {
                heavyMovementChecks = true;
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void PlayerFlight(PlayerToggleFlightEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Objects
        p.setFlying(e.isCancelled() ? n.isFlying() : e.isFlying() || n.isFlying());
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
        PlayerData.update(p, n, false);
        boolean cancelled = e.isCancelled();

        if (!p.canDo(false)) {
            ServerFlying.run(p);
            return;
        }
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation to = p.setEventLocation(nto),
                from = new SpartanLocation(p, e.getFrom());
        p.processLastMoveEvent(to, from);

        // Objects
        to.retrieveDataFrom(p.getLocation());
        from.retrieveDataFrom(to);

        if (to.getYaw() != from.getYaw() || to.getPitch() != from.getPitch()) {
            p.setLastHeadMovement();
        }

        // Handlers
        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).process()) {
                return;
            }
        }

        // Values
        double preXZ = AlgebraUtils.getPreDistance(to.getX(), from.getX()) + AlgebraUtils.getPreDistance(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getPreDistance(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);
        boolean crawling = p.isCrawling();

        // Handlers
        MovementProcessing.run(p, dis, hor, ver, box, crawling);
        CombatProcessing.runMove(p, to);

        // Detections
        if (!crawling) {
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, Exploits.HEAD); // Optimised
            p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled); // Optimised
            p.getExecutor(Enums.HackType.KillAura).run(cancelled); // Optimised
        }
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, dis);

        if ((dis == 0.0 && p.getCustomDistance() == 0.0
                || dis < 0.01 && p.getCustomDistance() < 0.01 && ver == 0.0)
                && p.isOnGround() && p.isOnGroundCustom()) {
            return;
        }

        // Detections
        if (!crawling) {
            ServerFlying.run(p);
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, Exploits.CHUNK);
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, Speed.SPRINT);
        }
        if (heavyMovementChecks) {
            if (!crawling) {
                p.getExecutor(Enums.HackType.NoFall).run(cancelled);
                p.getExecutor(Enums.HackType.NoSlowdown).run(cancelled);
            }
            p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
            p.getExecutor(Enums.HackType.Speed).run(cancelled);
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, Speed.WATER);
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
        p.resetLocationData();

        // Detections
        CheckProtection.cancel(p.uuid, 20);

        // System
        Cache.clear(p, n, false, true, true, false, p.getLocation());
    }
}
