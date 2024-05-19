package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.protocol.ProtocolStorage;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class SpartanPlayerMovement {

    private final SpartanPlayer parent;
    double schedulerDistance;
    private final List<Double>
            eventDistance,
            eventHorizontal,
            eventVertical,
            eventBox;
    private final Map<Long, List<SpartanLocation>> locations;
    private int
            airTicks;
    private long
            artificialSwimming,
            lastLiquidTicks;
    private Material lastLiquidMaterial;
    private SpartanLocation
            location,
            eventTo,
            eventFrom,
            detectionLocation;
    SpartanLocation schedulerFrom;
    private Vector clampVector;

    SpartanPlayerMovement(SpartanPlayer parent, Player p) {
        this.parent = parent;
        this.schedulerDistance = 0.0;

        this.eventDistance = Collections.synchronizedList(new LinkedList<>());
        this.eventHorizontal = Collections.synchronizedList(new LinkedList<>());
        this.eventVertical = Collections.synchronizedList(new LinkedList<>());
        this.eventBox = Collections.synchronizedList(new LinkedList<>());

        this.airTicks = 0;

        this.artificialSwimming = 0L;
        this.lastLiquidTicks = 0L;
        this.lastLiquidMaterial = Material.AIR;

        SpartanLocation location = new SpartanLocation(p.getLocation());
        this.locations = Collections.synchronizedMap(new LinkedHashMap<>());
        List<SpartanLocation> list = new ArrayList<>();
        list.add(location);
        this.locations.put(TPS.getTick(this.parent), list);

        this.location = location;
        this.eventTo = location;
        this.eventFrom = location;
        this.schedulerFrom = location;
        this.detectionLocation = location;

        this.clampVector = new Vector();
    }

    public double getSchedulerDistance() {
        return schedulerDistance;
    }

    // Separator

    public double getValueOrDefault(Double value, double def) {
        return value == null ? def : value;
    }

    // Separator

    public Double getEventDistance() {
        synchronized (eventDistance) {
            int size = eventDistance.size();
            return size > 0 ? eventDistance.get(size - 1) : null;
        }
    }

    public Double getPreviousEventDistance() {
        synchronized (eventDistance) {
            int size = eventDistance.size();
            return size > 1 ? eventDistance.get(size - 2) : null;
        }
    }

    // Separator

    public Double getEventHorizontal() {
        synchronized (eventHorizontal) {
            int size = eventHorizontal.size();
            return size > 0 ? eventHorizontal.get(size - 1) : null;
        }
    }

    public Double getPreviousEventHorizontal() {
        synchronized (eventHorizontal) {
            int size = eventHorizontal.size();
            return size > 1 ? eventHorizontal.get(size - 2) : null;
        }
    }

    // Separator

    public Double getEventVertical() {
        synchronized (eventVertical) {
            int size = eventVertical.size();
            return size > 0 ? eventVertical.get(size - 1) : null;
        }
    }

    public Double getPreviousEventVertical() {
        synchronized (eventVertical) {
            int size = eventVertical.size();
            return size > 1 ? eventVertical.get(size - 2) : null;
        }
    }

    // Separator

    public Double getEventBox() {
        synchronized (eventBox) {
            int size = eventBox.size();
            return size > 0 ? eventBox.get(size - 1) : null;
        }
    }

    public Double getPreviousEventBox() {
        synchronized (eventBox) {
            int size = eventBox.size();
            return size > 1 ? eventBox.get(size - 2) : null;
        }
    }

    // Separator

    public boolean wasInLiquids() {
        return this.isSwimming()
                || TPS.getTick(this.parent) - this.lastLiquidTicks <=
                Math.max(Latency.getRoundedDelay(this.parent), 5L);
    }

    public Material getLastLiquidMaterial() {
        return lastLiquidMaterial;
    }

    public void setLastLiquid(Material material) {
        lastLiquidTicks = TPS.getTick(this.parent);
        lastLiquidMaterial = material;
    }

    public void removeLastLiquidTime() {
        lastLiquidTicks = 0L;
    }

    // Separator

    public boolean isSwimming() {
        if (artificialSwimming >= TPS.getTick(this.parent)) {
            return true;
        } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            Player p = this.parent.getInstance();
            return p != null && p.isSwimming();
        } else {
            return false;
        }
    }

    public void setArtificialSwimming() {
        this.artificialSwimming = TPS.getTick(this.parent) + 1;
    }

    // Separator

    public boolean isLowEyeHeight() {
        return this.parent.getEyeHeight() < 1.0;
    }

    public boolean isCrawling() {
        return this.isLowEyeHeight() && !isGliding() && !this.isSwimming();
    }

    public boolean isWalkJumping(double vertical) {
        return !this.isSprinting()
                && this.getValueOrDefault(this.getEventDistance(), 0.0) > 0.0
                && this.isJumping(vertical);
    }

    public boolean isSneaking() {
        if (SpartanBukkit.packetsEnabled()) {
            return ProtocolStorage.getAbilities(this.parent.uuid).sneak();
        } else {
            Player p = this.parent.getInstance();
            return p != null && p.isSneaking();
        }
    }

    public boolean isFlying() {
        Entity vehicle = this.parent.getVehicle();

        if (vehicle != null) {
            return vehicle instanceof Player && ((Player) vehicle).isFlying();
        } else {
            Player p = this.parent.getInstance();
            return p != null && p.isFlying();
        }
    }

    // Separator

    public boolean isSprinting() {
        if (SpartanBukkit.packetsEnabled()) {
            return ProtocolStorage.getAbilities(this.parent.uuid).sprint();
        } else {
            Player p = this.parent.getInstance();
            return p != null && p.isSprinting();
        }
    }

    public boolean isSprintJumping(double vertical) {
        return this.isSprinting() && this.isJumping(vertical);
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

    public int getFallTick(double d, double acceleration, double drag, double precision) {
        return PlayerUtils.getFallTick(
                d,
                acceleration,
                drag,
                precision,
                PlayerUtils.getPotionLevel(this.parent, PotionEffectType.JUMP) + 1
        );
    }

    public boolean isFalling(double d, double acceleration, double drag, double precision) {
        return getFallTick(
                d,
                -acceleration,
                drag,
                precision
        ) != -1;
    }

    // Separator

    public boolean isGliding() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Player p = this.parent.getInstance();
            return p != null && p.isGliding();
        } else {
            return false;
        }
    }

    // Separator

    public int getTicksOnAir() {
        return airTicks;
    }

    // Separator

    public SpartanLocation getSchedulerFromLocation() {
        return schedulerFrom;
    }

    public SpartanLocation getEventToLocation() {
        return eventTo;
    }

    public SpartanLocation getEventFromLocation() {
        return eventFrom;
    }

    // Separator

    public List<SpartanLocation> getLocations() {
        List<SpartanLocation> toReturn = new ArrayList<>();

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

    public void setDetectionLocation(boolean force) {
        SpartanLocation location = this.getLocation();

        if (force
                || !location.world.equals(this.detectionLocation.world)
                || location.distance(this.detectionLocation) > 4.0) {
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
        return this.eventTo.getX() != this.eventFrom.getX()
                || this.eventTo.getY() != this.eventFrom.getY()
                || this.eventTo.getZ() != this.eventFrom.getZ();
    }

    public boolean directionChanged() {
        return this.eventTo.getYaw() != this.eventFrom.getYaw()
                || this.eventTo.getPitch() != this.eventFrom.getPitch();
    }

    // Separator

    public SpartanLocation getVehicleLocation(Player player) {
        Entity vehicle = player.getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location playerLocation = player.getLocation();
            return new SpartanLocation(
                    vehicle.getLocation(),
                    playerLocation.getYaw(),
                    playerLocation.getPitch());
        }
        return null;
    }

    public SpartanLocation getLocation() {
        Player p = this.parent.getInstance();

        if (p != null) {
            SpartanLocation spartanLocation = getVehicleLocation(p);

            if (spartanLocation == null) {
                SpartanLocation defaultLocation = new SpartanLocation(p.getLocation()),
                        actualLocation = SpartanBukkit.packetsEnabled()
                        ? ProtocolStorage.getLocation(this.parent.uuid, defaultLocation)
                        : defaultLocation;

                if (this.location.getX() != actualLocation.getX()
                        || this.location.getY() != actualLocation.getY()
                        || this.location.getZ() != actualLocation.getZ()
                        || this.location.getYaw() != actualLocation.getYaw()
                        || this.location.getPitch() != actualLocation.getPitch()) {
                    SpartanLocation from = this.location.clone();
                    this.setLocation(actualLocation, false);
                    location.retrieveDataFrom(from);
                }
            }
        }
        return location;
    }

    private void setLocation(SpartanLocation location, boolean queue) {
        if (queue) {
            synchronized (this.locations) {
                if (this.locations.size() == TPS.maximum) {
                    Iterator<Long> iterator = this.locations.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                this.locations.computeIfAbsent(TPS.getTick(this.parent), k -> new ArrayList<>()).add(location);
            }
        }
        if (!this.location.world.equals(location.world)) {
            this.parent.resetTrackers();
        }
        this.location = location;
    }

    // Separator

    public boolean processLastMoveEvent(SpartanLocation to, SpartanLocation from,
                                        double distance, double horizontal, double vertical, double box) {
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
        this.setLocation(to, true);
        this.eventTo = to;
        this.eventFrom = from;
        this.eventDistance.add(distance);
        this.eventHorizontal.add(horizontal);
        this.eventVertical.add(vertical);
        this.eventBox.add(box);

        for (List list : new List[]{
                eventDistance,
                eventHorizontal,
                eventVertical,
                eventBox
        }) {
            if (list.size() > 2) {
                list.remove(0);
            }
        }
        this.judgeGround(true);
        return true;
    }

    private double clampMin(double d, double d2, double d3) {
        return d < d2 ? d2 : Math.min(d, d3);
    }

    // Separator

    private void judgeGround(boolean increase) {
        if (this.parent.isOnGround()) {
            this.airTicks = 0;
        } else if (increase) {
            this.airTicks++;
        }
    }

    public void judgeGround() {
        this.judgeGround(false);
    }
}