package me.vagdedes.spartan.features.protections;

import me.vagdedes.spartan.checks.exploits.RapidDisallowedBuilding;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;

public class Building {

    private static final String key = "building=protection";
    public static final double maxMovingSpeed = 0.15;
    private static final Collection<Enums.HackType> concerningHackTypes;

    private static final double[]
            current = new double[]{
            BlockUtils.hitbox_max, -1.5, BlockUtils.hitbox_max
    }, extra = new double[]{
            BlockUtils.hitbox_extra, -1.5, BlockUtils.hitbox_extra
    };

    static {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        concerningHackTypes = new HashSet<>(hackTypes.length);

        for (Enums.HackType hackType : hackTypes) {
            if (hackType.getCheck().getCheckType() == Enums.CheckType.WORLD) {
                concerningHackTypes.add(hackType);
            }
        }
    }

    public static void runInteract(SpartanPlayer p, SpartanBlock clicked, Action action) {
        if (action != Action.RIGHT_CLICK_BLOCK || clicked == null || p.isFlying()) {
            return;
        }
        SpartanLocation loc = p.getLocation(),
                cloc = clicked.getLocation();
        double distance = loc.distance(cloc);

        if (loc.getY() >= (cloc.getY() + 0.5) && distance >= 0.5 && distance <= 3.0) {
            SpartanInventory inventory = p.getInventory();

            if ((inventory.getItemInHand().getType().isBlock()
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && inventory.getItemInOffHand().getType().isBlock())

                    && loc.getBlockX() == cloc.getBlockX() && loc.getBlockZ() == cloc.getBlockZ()
                    && !BlockUtils.isSolid(cloc.clone().add(0, 1, 0)) // Above
                    && (p.isSneaking() || !BlockUtils.isChangeable(cloc)) && distance > 2.0 && distance < 2.5) {
                p.getHandlers().add(Handlers.HandlerType.Building, 60);
            }
        }
    }

    public static void runPlace(SpartanPlayer p, SpartanBlock b, BlockFace blockFace, boolean cancelled) {
        if (p.isFlying() || Permissions.isBypassing(p, null)) {
            return;
        }
        Timer timer = p.getTimer();
        Handlers handlers = p.getHandlers();
        SpartanLocation loc = p.getLocation();

        if (cancelled) {
            long ms = timer.get(key);

            if ((ms <= 50L || blockFace == BlockFace.SELF)
                    && (Enums.HackType.FastPlace.getCheck().getViolations(p).hasLevel()
                    || p.getBuffer().start("building=protection=attempts", 20) >= 5)) {
                handlers.remove(Handlers.HandlerType.Building);
                handlers.add(Handlers.HandlerType.Building, "cooldown", 10);

                if (TestServer.isIdentified() || Settings.getBoolean("Protections.disallowed_building")) {
                    teleport(p, loc);
                    p.groundTeleport(false);
                }
                RapidDisallowedBuilding.run(p);
            }
        }
        timer.set(key);

        if (!handlers.has(Handlers.HandlerType.Building, "cooldown")
                && (!p.isOnGround() || !p.isOnGroundCustom())) {
            int cases = 0;
            SpartanLocation bloc = b.getLocation();
            SpartanLocation ploc = loc.clone().add(0, -1, 0);
            int blocY = bloc.getBlockY();

            if (ploc.getBlockX() == bloc.getBlockX()
                    && ploc.getBlockY() == blocY
                    && ploc.getBlockZ() == bloc.getBlockZ()) {
                cases = 1;
            } else if (PatternUtils.isBlockPattern(current, loc, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                cases = 2;
            } else if (PatternUtils.isBlockPattern(extra, loc, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                cases = 3;
            } else {
                ItemStack item = p.getItemInHand();

                if (item != null && (item.getType() == Material.WATER_BUCKET || item.getType() == Material.LAVA_BUCKET)) {
                    cases = 4;
                }
            }

            if (cases == 1 && cancelled) {
                if (TestServer.isIdentified() || Settings.getBoolean("Protections.disallowed_building")) {
                    handlers.add(Handlers.HandlerType.Building, 10);
                    teleport(p, loc);
                } else {
                    handlers.add(Handlers.HandlerType.Building, 60);
                }
            } else if (cases != 0) {
                handlers.add(Handlers.HandlerType.Building, 60);

                // Ground Utility Jumping Handler
                if (blocY <= loc.getBlockY()) {
                    double y = p.getNmsVerticalDistance();

                    if (p.isJumping(y)) {
                        GroundUtils.setOnGround(p, 12);
                    } else if (y < 0.0 || !cancelled) {
                        GroundUtils.setOnGround(p, 8);
                    }
                } else if (!cancelled) {
                    GroundUtils.setOnGround(p, 8);
                }
            }
        }
    }

    public static boolean hasCooldown(SpartanPlayer p) {
        if (p.getHandlers().has(Handlers.HandlerType.Building)
                && (p.getNmsDistance() <= maxMovingSpeed
                || Damage.hasCooldown(p)
                || PlayerData.isInActivePlayerCombat(p))) {
            return !p.getProfile().isSuspectedOrHacker(concerningHackTypes);
        }
        return false;
    }

    private static void teleport(SpartanPlayer p, SpartanLocation loc) {
        p.getHandlers().disable(Handlers.HandlerType.Teleport, 1);
        p.teleport(new SpartanLocation(p, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0.0f, loc));
    }
}
