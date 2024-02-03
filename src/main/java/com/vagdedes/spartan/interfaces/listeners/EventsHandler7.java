package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.checks.exploits.Exploits;
import com.vagdedes.spartan.checks.inventory.ImpossibleInventory;
import com.vagdedes.spartan.checks.movement.MorePackets;
import com.vagdedes.spartan.checks.movement.NoFall;
import com.vagdedes.spartan.checks.movement.NoSlowdown;
import com.vagdedes.spartan.checks.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.checks.movement.speed.Speed;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.moderation.Debug;
import com.vagdedes.spartan.functionality.protections.ServerFlying;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.tracking.CombatProcessing;
import com.vagdedes.spartan.handlers.tracking.MovementProcessing;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.data.Decimals;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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
        p.setFlying(e.isCancelled() ? n.isFlying() : e.isFlying() || n.isFlying(), n.getAllowFlight());
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
        Decimals decimals = p.getDecimals();

        if (e.isCancelled()) {
            return;
        }
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
                rem = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);
        Buffer buffer = p.getBuffer();
        int tick = buffer.start("move-event", 1);
        boolean crawling = p.isCrawling(),
                firstTick = tick == 1,
                longDistance = dis >= MoveUtils.nearMaxFallingMotion,
                repetitionDistance = firstTick || longDistance,
                repetition = repetitionDistance || p.getProfile().isSuspectedOrHacker(handledChecks),
                checkPlayer = longDistance || !p.canSkipDetectionTick(SpartanLocation.clearanceTick);

        // Handlers
        Cooldowns cooldowns = p.getCooldowns();
        CombatProcessing.runMove(p, to, cooldowns, decimals);

        // Detections
        if (checkPlayer && !crawling) {
            p.getExecutor(Enums.HackType.Exploits).handle(Exploits.HEAD); // Optimised
            p.getExecutor(Enums.HackType.ImpossibleInventory).run(); // Optimised
            p.getExecutor(Enums.HackType.KillAura).run(); // Optimised
        }
        p.getExecutor(Enums.HackType.Exploits).handle(dis);

        if (!repetitionDistance
                && (dis == 0.0 || dis < 0.01 && ver == 0.0)
                && p.isOnGround() && p.isOnGroundCustom()) {
            return;
        }

        // Values
        int air = p.getTicksOnAir();
        Entity vehicle = p.getVehicle();
        boolean hasVehicle = vehicle != null;

        // Features
        if (Debug.canRun()) {
            Debug.inform(p, Enums.Debug.MOVEMENT, "distance: " + AlgebraUtils.cut(dis, 2) + ", "
                    + "vertical: " + AlgebraUtils.cut(ver, 2) + ", "
                    + "horizontal: " + AlgebraUtils.cut(hor, 2) + ", "
                    + "remaining: " + AlgebraUtils.cut(rem, 5) + ", "
                    + "air-ticks: " + air + ", "
                    + "ground(vanilla/custom): " + p.isOnGround() + "/" + p.isOnGroundCustom()
                    + (hasVehicle ? ", vehicle: " + CombatUtils.entityToString(vehicle) : ""));
        }

        // Utils
        MovementProcessing.run(p, to, vehicle, buffer, cooldowns, decimals, dis, hor, ver, rem, crawling); // Optimised

        // Detections
        if (checkPlayer && !crawling && (firstTick || repetition)) {
            if (firstTick) {
                ServerFlying.run(p);
            }
            p.getExecutor(Enums.HackType.Exploits).handle(Exploits.CHUNK); // Optimised
            p.getExecutor(Enums.HackType.Speed).handle(Speed.SPRINT); // Optimised
        }

        if (heavyMovementChecks) {
            if (checkPlayer && (firstTick || repetition || tick <= 3)) {
                // Detections
                if (firstTick || repetition) {
                    if (!crawling) {
                        p.getExecutor(Enums.HackType.NoFall).run(); // Repetitive (Optimised)
                        p.getExecutor(Enums.HackType.NoSlowdown).run(); // Repetitive (Optimised)
                        //p.getExecutor(Enums.HackType.EntityMove).run(); // Repetitive (Optimised)
                    }
                    p.getExecutor(Enums.HackType.IrregularMovements).run(); // Repetitive (Optimised)
                }
                p.getExecutor(Enums.HackType.Speed).run(); // Important (Optimised)
                p.getExecutor(Enums.HackType.Speed).handle(Speed.WATER); // Repetitive (Optimised)
            }
            p.getExecutor(Enums.HackType.MorePackets).run(); // Required (Let the check run in all circumstances to accurately count the packets)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldChange(PlayerChangedWorldEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Object
        p.resetLocationData();
        p.setPlayerWeather(n.getPlayerWeather());

        // Detections
        CheckProtection.cancel(p.getUniqueId(), 20, true);

        // System
        Cache.clear(p, n, false, true, true, p.getLocation());
    }
}
