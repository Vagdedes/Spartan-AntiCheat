package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.Check;
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
        if (cancelled) {
            Timer timer = p.getTimer();
            long ms = timer.get(key);
            timer.set(key);

            if ((ms <= TPS.tickTime
                    || blockFace == BlockFace.SELF)
                    && (p.getViolations(Enums.HackType.FastPlace).hasLevel()
                    || p.getBuffer().start("building=protection=attempts", 20, Check.detectionMeasurementTicks) >= 0.25)) {
                if (Config.settings.getBoolean("Protections.disallowed_building")) {
                    teleport(p, p.movement.getLocation());
                    p.groundTeleport();
                }
                return;
            }
        }

        SpartanLocation loc = p.movement.getLocation(), bloc = b.getLocation();

        if (Math.abs(loc.getBlockX() - bloc.getBlockX()) <= 1
                && bloc.getBlockY() <= loc.getBlockY()
                && Math.abs(loc.getBlockZ() - bloc.getBlockZ()) <= 1) {
            boolean offGround = !p.isOnGround() || p.movement.getTicksOnAir() > 0;

            if (cancelled) {
                if (offGround
                        && Config.settings.getBoolean("Protections.disallowed_building")) {
                    teleport(p, loc);
                }
            }
        }
    }

    private static void teleport(SpartanPlayer p, SpartanLocation loc) {
        p.teleport(new SpartanLocation(p, loc.world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0.0f, loc));
    }
}
