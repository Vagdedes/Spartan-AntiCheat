package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.*;

public class SpartanPlayerMovement {

    private final SpartanPlayer parent;
    private Double
            eventDistance,
            eventHorizontal,
            eventPreviousHorizontal,
            eventVertical,
            eventXDiff,
            eventZDiff,
            eventPreviousVertical,
            eventBox,
            eventPreviousBox;
    private final Map<Long, List<SpartanLocation>> locations;
    private int
            airTicks;
    private long
            lastFlight,
            lastGlide,
            artificialSwimming,
            lastLiquidTicks;
    private Material lastLiquidMaterial;
    private SpartanLocation
            location,
            eventFrom,
            detectionLocation;
    SpartanLocation schedulerFrom;
    private Vector clampVector;

    SpartanPlayerMovement(SpartanPlayer parent) {
        this.parent = parent;
        this.lastLiquidMaterial = Material.AIR;

        SpartanLocation location = new SpartanLocation(
                ProtocolLib.getLocation(parent.getInstance())
        );
        this.locations = Collections.synchronizedMap(new LinkedHashMap<>());
        List<SpartanLocation> list = new ArrayList<>();
        list.add(location);
        this.locations.put(TPS.tick(), list);

        this.location = location;
        this.eventFrom = location;
        this.schedulerFrom = location;
        this.detectionLocation = location;

        this.clampVector = new Vector();
    }

    // Separator

    public double getValueOrDefault(Double value, double def) {
        return value == null ? def : value;
    }

    // Separator

    public Double getEventDistance() {
        return eventDistance;
    }

    // Separator

    public Double getEventHorizontal() {
        return eventHorizontal;
    }

    public Double getPreviousEventHorizontal() {
        return eventPreviousHorizontal;
    }

    // Separator

    public Double getEventXDiff() {
        return eventXDiff;
    }

    public Double getEventZDiff() {
        return eventZDiff;
    }

    // Separator

    public Double getEventVertical() {
        return eventVertical;
    }

    public Double getPreviousEventVertical() {
        return eventPreviousVertical;
    }

    // Separator

    public Double getEventBox() {
        return eventBox;
    }

    public Double getPreviousEventBox() {
        return eventPreviousBox;
    }

    // Separator

    public boolean isInLiquids() {
        return this.isSwimming()
                || TPS.tick() - this.lastLiquidTicks <=
                Math.max(AlgebraUtils.integerCeil(Latency.getDelay(this.parent)), 5L);
    }

    public Material getLastLiquidMaterial() {
        return lastLiquidMaterial;
    }

    public void setLastLiquid(Material material) {
        lastLiquidTicks = TPS.tick();
        lastLiquidMaterial = material;
    }

    public void removeLastLiquidTime() {
        lastLiquidTicks = 0L;
    }

    // Separator

    public boolean isSwimming() {
        if (artificialSwimming >= TPS.tick()) {
            return true;
        } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            return this.parent.getInstance().isSwimming();
        } else {
            return false;
        }
    }

    public void setArtificialSwimming() {
        this.artificialSwimming = TPS.tick() + 1;
    }

    // Separator

    public boolean isLowEyeHeight() {
        return this.parent.getInstance().getEyeHeight() < 1.0;
    }

    public boolean isWalkJumping(double vertical) {
        return !this.parent.protocol.isSprinting()
                && this.getValueOrDefault(this.getEventDistance(), 0.0) > 0.0
                && this.justJumped(vertical);
    }

    public boolean isFlying() {
        Entity vehicle = this.parent.getInstance().getVehicle();
        boolean flying;

        if (vehicle != null) {
            flying = vehicle instanceof Player && ((Player) vehicle).isFlying();
        } else {
            flying = this.parent.getInstance().isFlying();
        }
        if (flying) {
            this.lastFlight = System.currentTimeMillis();
        }
        return flying;
    }

    public boolean wasFlying() {
        return this.isFlying()
                || System.currentTimeMillis() - this.lastFlight <= TPS.maximum * TPS.tickTime;
    }

    // Separator

    public boolean isSprintJumping(double vertical) {
        return this.parent.protocol.isSprinting() && this.isJumping(vertical);
    }

    // Separator

    public boolean isJumping(double d) {
        return PlayerUtils.isJumping(
                d,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectUtils.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    public boolean justJumped(double d) {
        return PlayerUtils.justJumped(
                d,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectUtils.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    // Separator

    public double getAirAcceleration() {
        return this.location.isChunkLoaded()
                ? PlayerUtils.airAcceleration
                : PlayerUtils.airAccelerationUnloaded;
    }

    public int getFallTick(double d, double acceleration, double drag, double precision) {
        return PlayerUtils.getFallTick(
                d,
                acceleration,
                drag,
                precision,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectUtils.JUMP) + 1
        );
    }

    public boolean isFalling(double d, double acceleration, double drag, double precision) {
        return getFallTick(
                d,
                acceleration,
                drag,
                precision
        ) != -1;
    }

    public boolean justFell(double d) {
        return d < 0.0
                && Math.abs((0.0 - d) - (this.getAirAcceleration() * PlayerUtils.airDrag))
                <= GroundUtils.maxHeightLengthRatio;
    }

    public boolean justLanded(double now, double before, double acceleration, double drag, double precision) {
        return !isFalling(now, acceleration, drag, precision)
                && isFalling(before, acceleration, drag, precision);
    }

    // Separator

    public boolean isGliding() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            if (this.parent.getInstance().isGliding()) {
                this.lastGlide = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean wasGliding() {
        return this.isGliding()
                || System.currentTimeMillis() - this.lastGlide <= TPS.maximum * TPS.tickTime;
    }

    // Separator

    public int getTicksOnAir() {
        return airTicks;
    }

    // Separator

    public SpartanLocation getSchedulerFromLocation() {
        return schedulerFrom;
    }

    public SpartanLocation getEventFromLocation() {
        return eventFrom;
    }

    // Separator

    public List<SpartanLocation> getLocations() {
        List<SpartanLocation> toReturn = new ArrayList<>(this.locations.size());

        synchronized (this.locations) {
            for (List<SpartanLocation> list : locations.values()) {
                toReturn.addAll(list);
            }
        }
        return toReturn;
    }

    public Set<Map.Entry<Long, List<SpartanLocation>>> getLocationEntries() {
        synchronized (this.locations) {
            return new HashSet<>(locations.entrySet());
        }
    }

    // Separator

    public SpartanLocation getDetectionLocation() {
        return detectionLocation;
    }

    public void setDetectionLocation() {
        this.setDetectionLocation(this.getLocation(), true);
    }

    private void setDetectionLocation(SpartanLocation location, boolean force) {
        if (force
                || !location.world.equals(this.detectionLocation.world)
                || location.distance(this.detectionLocation) > 4.0) {
            this.detectionLocation = location;
        }
    }

    // Separator

    public boolean positionChanged() {
        return this.location.getX() != this.eventFrom.getX()
                || this.location.getY() != this.eventFrom.getY()
                || this.location.getZ() != this.eventFrom.getZ();
    }

    public boolean directionChanged() {
        return this.location.getYaw() != this.eventFrom.getYaw()
                || this.location.getPitch() != this.eventFrom.getPitch();
    }

    // Separator

    public SpartanLocation getVehicleLocation() {
        Entity vehicle = this.parent.getInstance().getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location playerLocation = this.parent.protocol.getLocation();
            return new SpartanLocation(
                    ProtocolLib.getLocation(vehicle),
                    playerLocation.getYaw(),
                    playerLocation.getPitch());
        }
        return null;
    }

    public SpartanLocation getLocation() {
        SpartanLocation vehicleLocation = getVehicleLocation();
        this.refreshLocation(this.parent.protocol.getLocation());

        if (vehicleLocation == null) {
            return this.location;
        } else {
            return vehicleLocation;
        }
    }

    public SpartanLocation getRawLocation() {
        return this.location;
    }

    // Separator

    public Location refreshLocation(Location location) {
        if (this.location.getX() != location.getX()
                || this.location.getY() != location.getY()
                || this.location.getZ() != location.getZ()
                || this.location.getYaw() != location.getYaw()
                || this.location.getPitch() != location.getPitch()) {
            SpartanLocation spartanLocation = new SpartanLocation(location);

            synchronized (this.locations) {
                if (this.locations.size() == TPS.maximum) {
                    Iterator<Long> iterator = this.locations.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                this.locations.computeIfAbsent(TPS.tick(), k -> new ArrayList<>()).add(spartanLocation);
            }
            this.location = spartanLocation;
            this.setDetectionLocation(spartanLocation, false);
        }
        return location;
    }

    // Separator

    public boolean processLastMoveEvent(Location originalTo, SpartanLocation vehicle,
                                        SpartanLocation to, SpartanLocation from,
                                        double distance, double horizontal,
                                        double vertical, double xDiff, double zDiff,
                                        double box) {
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
        if (vehicle == null) {
            this.refreshLocation(originalTo);
        }
        this.eventFrom = from;
        this.eventDistance = distance;
        this.eventPreviousHorizontal = this.eventHorizontal;
        this.eventHorizontal = horizontal;
        this.eventPreviousVertical = this.eventVertical;
        this.eventVertical = vertical;
        this.eventXDiff = xDiff;
        this.eventZDiff = zDiff;
        this.eventPreviousBox = this.eventBox;
        this.eventBox = box;
        this.judgeGround(true);
        return this.eventPreviousHorizontal != null
                && this.eventPreviousVertical != null
                && this.eventPreviousBox != null;
    }

    private double clampMin(double d, double d2, double d3) {
        return d < d2 ? d2 : Math.min(d, d3);
    }

    // Separator

    private void judgeGround(boolean increase) {
        if (this.parent.isOnGround(false)) {
            this.resetAirTicks();
        } else if (increase) {
            this.airTicks++;
        }
    }

    public void judgeGround() {
        this.judgeGround(false);
    }

    public void resetAirTicks() {
        this.airTicks = 0;
    }

    // Separator

    public boolean isInVoid() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                ? this.location.getY() < this.location.world.getMinHeight()
                : this.location.getY() < 0;
    }

}