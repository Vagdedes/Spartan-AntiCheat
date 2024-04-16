package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Liquid;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.potion.PotionEffectType;

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
    private final List<Float> nmsFall;
    private long
            lastOffSprint,
            lastFall,
            lastJump,
            swimmingTime,
            flyingTime,
            lastLiquidTime;
    private int
            airTicks, oldAirTicks,
            fallingTicks,
            groundTicks,
            extraPackets;
    private long lastHeadMovement;
    private boolean gliding, swimming, sprinting, sneaking, flying;
    private SpartanLocation location, fromLocation, eventTo, eventFrom, detectionLocation;

    SpartanPlayerMovement(SpartanPlayer parent, Player p) {
        this.parent = parent;
        this.customDistance = 0.0;
        this.customHorizontalDistance = 0.0;
        this.customVerticalDistance = 0.0;

        this.nmsDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsHorizontalDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsVerticalDistance = Collections.synchronizedList(new LinkedList<>());
        this.nmsBox = Collections.synchronizedList(new LinkedList<>());

        this.nmsFall = Collections.synchronizedList(new LinkedList<>());

        this.lastOffSprint = 0L;
        this.lastFall = 0L;
        this.lastJump = 0L;
        this.swimmingTime = 0L;
        this.flyingTime = 0L;
        this.lastLiquidTime = 0L;

        this.airTicks = 0;
        this.oldAirTicks = 0;
        this.fallingTicks = 0;
        this.groundTicks = 0;
        this.extraPackets = 0;

        this.lastHeadMovement = System.currentTimeMillis();

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
        this.fromLocation = this.location;
        this.eventTo = this.location;
        this.eventFrom = this.location;
        this.detectionLocation = this.location;
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
                                            double box,
                                            float fall) {
        this.nmsDistance.add(distance);
        this.nmsHorizontalDistance.add(horizontal);
        this.nmsVerticalDistance.add(vertical);
        this.nmsBox.add(box);
        this.nmsFall.add(fall);

        for (List list : new List[]{
                nmsDistance,
                nmsHorizontalDistance,
                nmsVerticalDistance,
                nmsBox,
                nmsFall
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

    public float getValueOrDefault(Float value, float def) {
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

    public Float getNmsFall() {
        synchronized (nmsFall) {
            int size = nmsFall.size();
            return size > 0 ? nmsFall.get(size - 1) : null;
        }
    }

    public Float getPreviousNmsFall() {
        synchronized (nmsFall) {
            int size = nmsFall.size();
            return size > 1 ? nmsFall.get(size - 2) : null;
        }
    }

    // Separator

    public synchronized void setLastHeadMovement() {
        this.lastHeadMovement = System.currentTimeMillis();
    }

    // Separator

    public boolean wasInLiquids() {
        return System.currentTimeMillis() - lastLiquidTime <= 755L;
    }

    public synchronized void setLastLiquidTime() {
        lastLiquidTime = System.currentTimeMillis();
    }

    public synchronized void removeLastLiquidTime() {
        lastLiquidTime = 0L;
    }

    // Separator

    public boolean isSwimming() {
        return swimming || swimmingTime >= System.currentTimeMillis();
    }

    public synchronized void setSwimming(boolean swimming, int ticks) {
        this.swimming = swimming;

        if (ticks != -1L) {
            this.swimmingTime = (ticks > 0 ? System.currentTimeMillis() + (ticks * 50L) : 0L);
        }
    }

    // Separator

    public boolean isMoving(boolean head) {
        if (!head || (System.currentTimeMillis() - this.lastHeadMovement) <= 2_500L) {
            Double nmsDistance = this.getNmsDistance();
            return nmsDistance != null && nmsDistance >= 0.1
                    || this.getCustomDistance() >= 0.1
                    || this.isSprinting()
                    || this.isSprintJumping()
                    || this.isWalkJumping()
                    || this.parent.getLastDamageReceived().ticksPassed() <= 2;
        } else {
            return false;
        }
    }

    public boolean isCrawling() {
        return this.parent.getEyeHeight() <= 1.2 && !isGliding() && !this.isSwimming();
    }

    public boolean isWalkJumping() {
        return !sprinting && this.getLastJump() <= (TPS.tickTime * 2L);
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

    public boolean isSprintJumping() {
        return sprinting && this.getLastJump() <= (TPS.tickTime * 2L);
    }

    public long getLastOffSprint() {
        return System.currentTimeMillis() - lastOffSprint;
    }

    public synchronized void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public synchronized void setLastOffSprint() {
        this.lastOffSprint = System.currentTimeMillis();
    }

    // Separator

    public double getExtraMovementFromJumpEffect() {
        return PlayerUtils.getPotionLevel(this.parent, PotionEffectType.JUMP) * 0.1;
    }

    public boolean isJumping(double d) {
        double precision = PlayerUtils.getJumpingPrecision(this.parent);
        return PlayerUtils.isJumping(
                d - this.getExtraMovementFromJumpEffect(),
                precision,
                this.parent.bedrockPlayer ? precision : 0.0
        );
    }

    public long getLastJump() {
        return System.currentTimeMillis() - this.lastJump;
    }

    public synchronized void setLastJump() {
        this.lastJump = System.currentTimeMillis();
        this.removeLastLiquidTime();
        this.parent.handlers.removeMany(Handlers.HandlerFamily.Velocity);

        if (!Liquid.isLocation(this.parent, this.getLocation())) {
            this.removeLastLiquidTime();
        }
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

    public boolean isClimbing(double d) {
        double v5 = AlgebraUtils.cut(d, 5);

        if (v5 != PlayerUtils.gravityAcceleration && v5 != 0.07544) {
            for (double value : PlayerUtils.climbing) {
                if (Math.abs(value - Math.abs(d)) < PlayerUtils.lowPrecision) {
                    return true;
                }
            }
        }
        return false;
    }

    // Separator

    public boolean isFalling(double dy) {
        return PlayerUtils.getFallingTick(dy) != -1;
    }

    public long getLastFall() {
        return System.currentTimeMillis() - this.lastFall;
    }

    public synchronized void setLastFall() {
        this.lastFall = System.currentTimeMillis();
    }

    // Separator

    public boolean isFlying() {
        Entity vehicle = this.parent.getVehicle();
        return vehicle != null ? vehicle instanceof Player && ((Player) vehicle).isFlying() : flying;
    }

    public boolean wasFlying() {
        return isFlying() || this.flyingTime >= System.currentTimeMillis();
    }

    public synchronized void setFlying(boolean flying) {
        if (!flying) {
            flying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                    && this.parent.getGameMode() == GameMode.SPECTATOR;
        }
        this.flying = flying;

        if (flying) {
            this.flyingTime = System.currentTimeMillis() + 3_000L;
        }
    }

    // Separator

    public int getExtraPackets() {
        return this.parent.isUsingItem() ? 0 : extraPackets;
    }

    public synchronized void setExtraPackets(int number) {
        this.extraPackets = number;
    }

    // Separator

    public int getTicksOnAir() {
        return airTicks;
    }

    public synchronized void setAirTicks(int number) {
        if (airTicks > 0) {
            setOldAirTicks(airTicks);
        }
        this.airTicks = number;

        if (number == 0) {
            this.fallingTicks = 0;
        }
    }

    public int getOldTicksOnAir() {
        return oldAirTicks;
    }

    public synchronized void setOldAirTicks(int number) {
        this.oldAirTicks = number;
    }

    // Separator

    public int getTicksOnGround() {
        return groundTicks;
    }

    public synchronized void setGroundTicks(int number) {
        this.groundTicks = number;

        if (number > 0) {
            this.fallingTicks = 0;
        }
    }

    // Separator

    public int getFallingTicks() {
        return fallingTicks;
    }

    public synchronized void setFallingTicks(int number) {
        this.fallingTicks = number;
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
                    SpartanLocation from = this.location.clone();
                    this.location = new SpartanLocation(this.parent, p.getLocation());
                    this.location.retrieveDataFrom(from);
                }
            }
            return this.location;
        }
    }

    public SpartanLocation getFromLocation() {
        return fromLocation;
    }

    public SpartanLocation getEventToLocation() {
        return eventTo;
    }

    public SpartanLocation getEventFromLocation() {
        return eventFrom;
    }

    public SpartanLocation getDetectionLocation() {
        return detectionLocation;
    }

    private SpartanLocation getVehicleLocation() {
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

    public void processLastMoveEvent(SpartanLocation to, SpartanLocation from) {
        this.eventTo = to;
        this.eventFrom = from;
    }

    public SpartanLocation setEventLocation(Location to) {
        SpartanLocation vehicle = getVehicleLocation();
        return vehicle != null ? vehicle : new SpartanLocation(this.parent, to);
    }

    public void setFromLocation(SpartanLocation loc) {
        this.fromLocation = loc;
    }

    // Separator

    public boolean stepsOnBoats() {
        return GroundUtils.stepsOnBoats(this.parent);
    }
}
