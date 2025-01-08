package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerMovement {

    private final SpartanPlayer parent;
    private final Map<Long, SpartanLocation> locations;
    private int
            airTicks;
    private long
            lastFlight,
            lastGlide,
            artificialSwimming,
            lastLiquidTicks;
    private Material lastLiquidMaterial;
    Location schedulerFrom;
    private Vector clampVector;
    public double motionY;

    PlayerMovement(SpartanPlayer parent) {
        this.parent = parent;
        this.lastLiquidMaterial = Material.AIR;

        Location bukkit = ProtocolTools.getLoadLocation(this.parent.protocol.bukkit());
        SpartanLocation location = new SpartanLocation(bukkit);
        this.locations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.locations.put(System.currentTimeMillis(), location);

        this.schedulerFrom = bukkit;

        this.clampVector = new Vector();
        this.motionY = 0;
    }

    // Separator

    public boolean isInLiquids() {
        return this.isSwimming()
                || System.currentTimeMillis() - this.lastLiquidTicks <= Math.max(
                this.parent.protocol.getPing(),
                5L * TPS.tickTime
        );
    }

    public Material getLastLiquidMaterial() {
        return lastLiquidMaterial;
    }

    public void setLastLiquid(Material material) {
        lastLiquidTicks = System.currentTimeMillis();
        lastLiquidMaterial = material;
    }

    public void removeLastLiquidTime() {
        lastLiquidTicks = 0L;
    }

    // Separator

    public boolean isSwimming() {
        if (artificialSwimming >= System.currentTimeMillis()) {
            return true;
        } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            return this.parent.protocol.bukkit().isSwimming();
        } else {
            return false;
        }
    }

    public void setArtificialSwimming() {
        this.artificialSwimming = System.currentTimeMillis() + TPS.tickTime;
    }

    // Separator

    public boolean isLowEyeHeight() {
        return this.parent.protocol.bukkit().getEyeHeight() < 1.0;
    }

    public boolean isFlying() {
        Entity vehicle = this.parent.protocol.spartan.getVehicle();
        boolean flying;

        if (vehicle != null) {
            flying = vehicle instanceof Player && ((Player) vehicle).isFlying();
        } else {
            flying = this.parent.protocol.bukkit().isFlying();
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
        return new SpartanLocation(this.parent.protocol.getLocationOrVehicle()).isChunkLoaded()
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

    // Separator

    public boolean isGliding() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            if (this.parent.protocol.bukkit().isGliding()) {
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

    public Location getSchedulerFromLocation() {
        return schedulerFrom;
    }

    public List<SpartanLocation> getLocations() {
        return new ArrayList<>(this.locations.values());
    }

    public Set<Map.Entry<Long, SpartanLocation>> getLocationEntries() {
        synchronized (this.locations) {
            return new HashSet<>(locations.entrySet());
        }
    }

    // Separator

    public Location refreshLocation(Location location) {
        Location known = this.parent.protocol.getLocation();

        if (known.getX() != location.getX()
                || known.getY() != location.getY()
                || known.getZ() != location.getZ()
                || known.getYaw() != location.getYaw()
                || known.getPitch() != location.getPitch()) {
            SpartanLocation spartanLocation = new SpartanLocation(location);

            synchronized (this.locations) {
                if (this.locations.size() == TPS.maximum) {
                    Iterator<Long> iterator = this.locations.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                this.locations.put(System.currentTimeMillis(), spartanLocation);
            }
        }
        return location;
    }

    // Separator

    public boolean processLastMoveEvent(
            Location originalTo,
            Location vehicle,
            SpartanLocation to,
            Location from,
            boolean packets
    ) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                || this.parent.protocol.isUsingVersionOrGreater(MultiVersion.MCVersion.V1_17)) {
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
        if (!packets) {
            this.parent.protocol.setFromLocation(from);
        }
        this.judgeGround(true);
        return true;
    }

    private double clampMin(double d, double d2, double d3) {
        return d < d2 ? d2 : Math.min(d, d3);
    }

    // Separator

    private boolean judgeGround(boolean increase) {
        if (this.parent.isOnGround(false)) {
            this.resetAirTicks();
            return true;
        } else {
            if (increase) {
                this.airTicks++;
            }
            return false;
        }
    }

    public boolean judgeGround() {
        return this.judgeGround(false);
    }

    public void resetAirTicks() {
        this.airTicks = 0;
    }

}