package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SpartanPlayerMovement {

    private final SpartanPlayer parent;
    private double
            customDistance, customHorizontalDistance, customVerticalDistance;
    private final List<Double>
            nmsDistance, nmsHorizontalDistance, nmsVerticalDistance,
            nmsBox;
    private long
            swimmingTime,
            lastLiquidTicks,
            lastOnGround,
            lastVanillaOnGround;
    private Material lastLiquidMaterial;
    private boolean gliding, swimming, sprinting, sneaking, flying;
    private SpartanLocation location, nmsTo, nmsFrom, detectionLocation;
    SpartanLocation customFromLocation;
    private Vector clampVector;

    SpartanPlayerMovement(SpartanPlayer parent, Player p) {
        this.parent = parent;
        this.customDistance = 0.0;
        this.customHorizontalDistance = 0.0;
        this.customVerticalDistance = 0.0;

        this.nmsDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsHorizontalDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsVerticalDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsBox = Collections.synchronizedList(new LinkedList<>());

        this.swimmingTime = 0L;
        this.lastLiquidTicks = 0L;
        this.lastLiquidMaterial = Material.AIR;
        this.lastOnGround = 0L;
        this.lastVanillaOnGround = 0;

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            this.gliding = p.isGliding();
            this.swimming = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && p.isSwimming();
        } else {
            this.gliding = false;
            this.swimming = false;
        }
        this.sprinting = p.isSprinting();
        this.sneaking = p.isSneaking();
        this.flying = p.isFlying();

        this.location = new SpartanLocation(this.parent, p.getLocation());
        this.customFromLocation = this.location;
        this.nmsTo = this.location;
        this.nmsFrom = this.location;
        this.detectionLocation = this.location;

        this.clampVector = new Vector();
    }

    public double getCustomDistance() {
        return customDistance;
    }

    public double getCustomHorizontalDistance() {
        return customHorizontalDistance;
    }

    public double getCustomVerticalDistance() {
        return customVerticalDistance;
    }

    // Separator

    public synchronized void setCustomDistance(double distance,
                                               double horizontal,
                                               double vertical) {
        this.customDistance = distance;
        this.customHorizontalDistance = horizontal;
        this.customVerticalDistance = vertical;
    }

    public synchronized void setNmsDistance(double distance,
                                            double horizontal,
                                            double vertical,
                                            double box) {
        this.nmsDistance.add(distance);
        this.nmsHorizontalDistance.add(horizontal);
        this.nmsVerticalDistance.add(vertical);
        this.nmsBox.add(box);

        for (List list : new List[]{
                nmsDistance,
                nmsHorizontalDistance,
                nmsVerticalDistance,
                nmsBox
        }) {
            if (list.size() > 2) {
                list.remove(0);
            }
        }
    }

    // Separator

    public double getValueOrDefault(Double value, double def) {
        return value == null ? def : value;
    }

    // Separator

    public Double getNmsDistance() {
        synchronized (nmsDistance) {
            int size = nmsDistance.size();
            return size > 0 ? nmsDistance.get(size - 1) : null;
        }
    }

    public Double getPreviousNmsDistance() {
        synchronized (nmsDistance) {
            int size = nmsDistance.size();
            return size > 1 ? nmsDistance.get(size - 2) : null;
        }
    }

    // Separator

    public Double getNmsHorizontalDistance() {
        synchronized (nmsHorizontalDistance) {
            int size = nmsHorizontalDistance.size();
            return size > 0 ? nmsHorizontalDistance.get(size - 1) : null;
        }
    }

    public Double getPreviousNmsHorizontalDistance() {
        synchronized (nmsHorizontalDistance) {
            int size = nmsHorizontalDistance.size();
            return size > 1 ? nmsHorizontalDistance.get(size - 2) : null;
        }
    }

    // Separator

    public Double getNmsVerticalDistance() {
        synchronized (nmsVerticalDistance) {
            int size = nmsVerticalDistance.size();
            return size > 0 ? nmsVerticalDistance.get(size - 1) : null;
        }
    }

    public Double getPreviousNmsVerticalDistance() {
        synchronized (nmsVerticalDistance) {
            int size = nmsVerticalDistance.size();
            return size > 1 ? nmsVerticalDistance.get(size - 2) : null;
        }
    }

    // Separator

    public Double getNmsBox() {
        synchronized (nmsBox) {
            int size = nmsBox.size();
            return size > 0 ? nmsBox.get(size - 1) : null;
        }
    }

    public Double getPreviousNmsBox() {
        synchronized (nmsBox) {
            int size = nmsBox.size();
            return size > 1 ? nmsBox.get(size - 2) : null;
        }
    }

    // Separator

    public boolean wasInLiquids() {
        return TPS.getTick(this.parent) - this.lastLiquidTicks <=
                Math.max(Latency.getRoundedDelay(this.parent), 5L);
    }

    public Material getLastLiquidMaterial() {
        return lastLiquidMaterial;
    }

    public synchronized void setLastLiquid(Material material) {
        lastLiquidTicks = TPS.getTick(this.parent);
        lastLiquidMaterial = material;
    }

    public synchronized void removeLastLiquidTime() {
        lastLiquidTicks = 0L;
    }

    // Separator

    public boolean isSwimming() {
        return swimming || swimmingTime >= TPS.getTick(this.parent);
    }

    public synchronized void setSwimming(boolean swimming, int ticks) {
        this.swimming = swimming;

        if (ticks > 0) {
            this.swimmingTime = TPS.getTick(this.parent) + ticks;
        }
    }

    public boolean isLowEyeHeight() {
        return this.parent.getEyeHeight() < 1.0;
    }

    public boolean isCrawling() {
        return this.isLowEyeHeight() && !isGliding() && !this.isSwimming();
    }

    public boolean isWalkJumping(double vertical) {
        return !sprinting && this.isJumping(vertical);
    }

    // Separator

    public boolean isSneaking() {
        return sneaking;
    }

    public synchronized void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    // Separator

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isSprintJumping(double vertical) {
        return sprinting && this.isJumping(vertical);
    }

    public synchronized void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    // Separator

    public boolean isJumping(double d) {
        return PlayerUtils.isJumping(
                d,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectType.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    public boolean justJumped(double d) {
        return PlayerUtils.justJumped(
                d,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectType.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    // Separator

    public int getFallTick(double d, double precision) {
        return PlayerUtils.getFallTick(
                d,
                precision,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectType.JUMP) + 1
        );
    }

    public boolean isFalling(double d, double precision) {
        return getFallTick(d, precision) != -1;
    }

    // Separator

    public boolean isGliding() {
        return gliding;
    }

    public synchronized void setGliding(boolean gliding, boolean modify) {
        this.gliding = gliding;

        if (modify && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Player p = this.parent.getPlayer();

            if (p != null) {
                p.setGliding(gliding);
            }
        }
    }

    // Separator

    public boolean isFlying() {
        Entity vehicle = this.parent.getVehicle();
        return vehicle != null ? vehicle instanceof Player && ((Player) vehicle).isFlying() : flying;
    }

    public synchronized void setFlying(boolean flying) {
        if (!flying) {
            flying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                    && this.parent.getGameMode() == GameMode.SPECTATOR;
        }
        this.flying = flying;

        if (flying) {
            this.parent.getTrackers().add(Trackers.TrackerType.FLIGHT, (int) TPS.maximum);
        }
    }

    // Separator

    public int getTicksOnAir() {
        long airTicks = TPS.getTick(this.parent) - this.lastOnGround,
                vanillaAirTicks = TPS.getTick(this.parent) - this.lastVanillaOnGround;
        return AlgebraUtils.integerRound(
                Math.sqrt(((airTicks * airTicks) + (vanillaAirTicks * vanillaAirTicks)) / 2.0)
        );
    }

    public synchronized void resetAirTicks() {
        this.lastOnGround = TPS.getTick(this.parent);
    }

    public synchronized void resetVanillaAirTicks() {
        this.lastVanillaOnGround = TPS.getTick(this.parent);
    }

    // Separator

    public SpartanLocation getLocation() {
        SpartanLocation vehicle = getVehicleLocation();

        if (vehicle != null) {
            return vehicle;
        } else {
            if (SpartanBukkit.isSynchronised()) {
                Player p = this.parent.getPlayer();

                if (p != null) {
                    Location bukkitLocation = p.getLocation();

                    if (this.location.getX() != bukkitLocation.getX()
                            || this.location.getY() != bukkitLocation.getY()
                            || this.location.getZ() != bukkitLocation.getZ()
                            || this.location.getYaw() != bukkitLocation.getYaw()
                            || this.location.getPitch() != bukkitLocation.getPitch()) {
                        SpartanLocation from = this.location.clone();
                        this.location = new SpartanLocation(this.parent, bukkitLocation);
                        this.location.retrieveDataFrom(from);
                    }
                }
            }
            return this.location;
        }
    }

    public SpartanLocation getCustomFromLocation() {
        return customFromLocation;
    }

    public SpartanLocation getNmsToLocation() {
        return nmsTo;
    }

    public SpartanLocation getNmsFromLocation() {
        return nmsFrom;
    }

    public SpartanLocation getDetectionLocation() {
        return detectionLocation;
    }

    public SpartanLocation getVehicleLocation() {
        Entity vehicle = this.parent.getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location vehicleLocation = vehicle.getLocation();
            SpartanLocation playerLocation = this.location;
            boolean isNull = playerLocation == null;
            return new SpartanLocation(this.parent,
                    vehicleLocation,
                    isNull ? vehicleLocation.getYaw() : playerLocation.getYaw(),
                    isNull ? vehicleLocation.getPitch() : playerLocation.getPitch());
        }
        return null;
    }

    public void setDetectionLocation(boolean force) {
        if (force
                || !this.location.world.equals(this.detectionLocation.world)
                || this.location.distance(this.detectionLocation) > PlayerUtils.terminalVelocity) {
            SpartanLocation location = this.getLocation();
            int minHeight = BlockUtils.getMinHeight(location.world);

            if (location.getBlockY() < minHeight) {
                location = location.clone();
                location.setY(minHeight);
            }
            this.detectionLocation = location;
        }
    }

    // Separator

    public boolean positionChanged() {
        return this.nmsTo.getX() != this.nmsFrom.getX()
                || this.nmsTo.getY() != this.nmsFrom.getY()
                || this.nmsTo.getZ() != this.nmsFrom.getZ();
    }

    public boolean directionChanged() {
        return this.nmsTo.getYaw() != this.nmsFrom.getYaw()
                || this.nmsTo.getPitch() != this.nmsFrom.getPitch();
    }

    // Separator

    public boolean processLastMoveEvent(SpartanLocation to, SpartanLocation from, double vertical) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            double x = clampMin(to.getX(), -3.0E7D, 3.0E7D),
                    y = clampMin(to.getY(), -2.0E7D, 2.0E7D),
                    z = clampMin(to.getZ(), -3.0E7D, 3.0E7D);
            Vector vector = new Vector(x, y, z);
            double d = this.clampVector.distanceSquared(vector);
            this.clampVector = vector;

            if (d < 4e-8) {
                return false;
            }
        }
        this.nmsTo = to;
        this.nmsFrom = from;
        return true;
    }

    private double clampMin(double d, double d2, double d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
    }
}
