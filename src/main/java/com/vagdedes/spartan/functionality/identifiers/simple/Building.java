package com.vagdedes.spartan.functionality.identifiers.simple;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.BlockFace;

public class Building {

    private static final String key = Building.class.getName();

    public static void runPlace(SpartanPlayer p, SpartanBlock b, BlockFace blockFace, boolean cancelled) {
        if (p.movement.isFlying() || Permissions.isBypassing(p, null)) {
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
                    || p.getBuffer().start("building=protection=attempts", 20, Check.detectionMeasurementTicks) >= 0.25)) {
                handlers.disable(Handlers.HandlerType.TowerBuilding, 10);

                if (Config.settings.getBoolean("Protections.disallowed_building")) {
                    teleport(p, p.movement.getLocation());
                    p.groundTeleport(false);
                }
                p.getExecutor(Enums.HackType.Exploits).handle(true, Exploits.BUILDING);
                return;
            }
        }

        SpartanLocation loc = p.movement.getLocation(), bloc = b.getLocation();

        if (Math.abs(loc.getBlockX() - bloc.getBlockX()) <= 1
                && bloc.getBlockY() <= loc.getBlockY()
                && Math.abs(loc.getBlockZ() - bloc.getBlockZ()) <= 1) {
            boolean offGround = !p.isOnGround() || !p.isOnGroundCustom() || p.movement.getTicksOnAir() > 0;

            if (cancelled) {
                if (Config.settings.getBoolean("Protections.disallowed_building")) {
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
        p.teleport(new SpartanLocation(p, loc.world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0.0f, loc));
    }
}
