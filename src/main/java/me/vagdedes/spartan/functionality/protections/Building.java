package me.vagdedes.spartan.functionality.protections;

import me.vagdedes.spartan.checks.exploits.Exploits;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.BlockFace;

public class Building {

    private static final String key = Building.class.getName();

    public static void runPlace(SpartanPlayer p, SpartanBlock b, BlockFace blockFace, boolean cancelled) {
        if (p.isFlying() || Permissions.isBypassing(p, null)) {
            return;
        }
        Handlers handlers = p.getHandlers();

        if (cancelled) {
            Timer timer = p.getTimer();
            long ms = timer.get(key);
            timer.set(key);

            if ((ms <= TPS.tickTime
                    || blockFace == BlockFace.SELF)
                    && (p.getViolations(Enums.HackType.FastPlace).hasLevel()
                    || p.getBuffer().start("building=protection=attempts", 20) >= 5)) {
                handlers.disable(Handlers.HandlerType.TowerBuilding, 10);

                if (TestServer.isIdentified()
                        || Config.settings.getBoolean("Protections.disallowed_building")) {
                    teleport(p, p.getLocation());
                    p.groundTeleport(false);
                }
                p.getExecutor(Enums.HackType.Exploits).handle(Exploits.BUILDING);
                return;
            }
        }

        SpartanLocation loc = p.getLocation(), bloc = b.getLocation();

        if (Math.abs(loc.getBlockX() - bloc.getBlockX()) <= 1
                && bloc.getBlockY() <= loc.getBlockY()
                && Math.abs(loc.getBlockZ() - bloc.getBlockZ()) <= 1) {
            boolean offGround = !p.isOnGround() || !p.isOnGroundCustom() || p.getTicksOnAir() > 0;

            if (cancelled) {
                if (TestServer.isIdentified()
                        || Config.settings.getBoolean("Protections.disallowed_building")) {
                    handlers.add(
                            offGround ? Handlers.HandlerType.TowerBuilding : Handlers.HandlerType.BridgeBuilding,
                            10
                    );

                    if (offGround) {
                        teleport(p, loc);
                    }
                } else {
                    handlers.add(
                            offGround ? Handlers.HandlerType.TowerBuilding : Handlers.HandlerType.BridgeBuilding,
                            10
                    );
                }
            } else {
                handlers.add(
                        offGround ? Handlers.HandlerType.TowerBuilding : Handlers.HandlerType.BridgeBuilding,
                        10
                );
            }
        }
    }

    private static void teleport(SpartanPlayer p, SpartanLocation loc) {
        p.getHandlers().disable(Handlers.HandlerType.Teleport, 1);
        p.teleport(new SpartanLocation(p, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0.0f, loc));
    }
}
