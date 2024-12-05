package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.component.ComponentY;
import com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets.TimerBalancer;
import com.vagdedes.spartan.abstraction.data.CheckBoundData;
import com.vagdedes.spartan.abstraction.data.PacketWorld;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.listeners.protocol.Packet_Teleport;
import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import java.util.*;

public class SpartanProtocol {

    public final long creationTime;
    public final Player bukkit;
    public final SpartanPlayer spartan;
    public final TimerBalancer timerBalancer;

    // Custom
    private boolean onGround, onGroundFrom;
    private int hashPosBuffer;
    public int rightClickCounter;
    public boolean mutateTeleport, sprinting, sneaking, vehicleStatus;
    public boolean simulationFlag, teleported, pistonTick;
    private Location location, from, teleport;
    public String fromWorld;
    public Location simulationStartPoint;
    private PlayerProfile profile;
    public List<Packet_Teleport.TeleportData> teleportEngine;
    public final MCClient mcClient;
    public byte simulationDelayPerTP, keepEntity;
    public LinkedList<Location> positionHistory, positionHistoryLong;
    public PlayerVelocityEvent claimedVelocity;
    public List<PlayerVelocityEvent> claimedVeloGravity;
    public long tickTime;
    public final MultiVersion.MCVersion version;

    private Set<AxisAlignedBB> axisMatrixCache;
    private CheckBoundData checkBoundData;
    public PacketWorld packetWorld;
    public long placeTime, placeHash;
    public BlockPlaceEvent oldBlockEvent;
    public PlayerTickEvent lastTickEvent;

    private ComponentY componentY;

    public SpartanProtocol(Player player) {
        long time = System.currentTimeMillis();
        this.creationTime = time;
        this.bukkit = player;
        this.version = MultiVersion.get(player);
        this.packetWorld = new PacketWorld(player);

        this.placeTime = time;
        this.placeHash = 0;
        this.onGround = false;
        this.onGroundFrom = false;
        this.sprinting = false;
        this.sneaking = false;
        this.mutateTeleport = false;
        this.location = ProtocolTools.getLoadLocation(player);
        this.from = null;
        this.fromWorld = "";
        this.teleport = null;
        this.teleportEngine = new LinkedList<>();
        this.simulationFlag = false;

        this.spartan = new SpartanPlayer(this);
        this.timerBalancer = new TimerBalancer();
        this.mcClient = new MCClient(player);
        this.simulationStartPoint = null;
        this.simulationDelayPerTP = 1;
        this.teleported = false;
        this.vehicleStatus = true;
        this.keepEntity = 0;
        this.tickTime = time;

        this.rightClickCounter = 0;
        this.positionHistory = new LinkedList<>();
        this.positionHistoryLong = new LinkedList<>();
        this.hashPosBuffer = 0;
        this.claimedVelocity = null;
        this.claimedVeloGravity = new ArrayList<>();

        this.axisMatrixCache = new HashSet<>();
        this.checkBoundData = null;
        this.oldBlockEvent = null;
        this.pistonTick = false;
        this.lastTickEvent = null;

        this.componentY = new ComponentY();

        this.setProfile(ResearchEngine.getPlayerProfile(this, false));
    }

    public long getTimePlayed() {
        return System.currentTimeMillis() - this.creationTime;
    }

    public boolean isUsingVersion(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() == trialVersion.ordinal();
    }

    public boolean isUsingVersionOrGreater(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() >= trialVersion.ordinal();
    }

    public void pushHashPosition(Location location) {
        this.hashPosBuffer = Objects.hashCode((location.getX() + location.getY() + location.getZ()));
    }

    public boolean isSameWithHash(Location location) {
        return this.hashPosBuffer == Objects.hashCode((location.getX() + location.getY() + location.getZ()));
    }

    public LinkedList<Location> getLocations() {
        return this.positionHistory;
    }

    public boolean isOnGround() {
        return packetsEnabled()
                ? this.onGround
                : this.bukkit.isOnGround();
    }

    public boolean isOnGroundFrom() {
        return packetsEnabled()
                ? this.onGroundFrom
                : this.bukkit.isOnGround();
    }

    public boolean isSprinting() {
        return packetsEnabled()
                ? this.sprinting
                : this.bukkit.isSprinting();
    }

    public boolean isSneaking() {
        return packetsEnabled()
                ? this.sneaking
                : this.bukkit.isSneaking();
    }

    public Location getLocation() {
        if (packetsEnabled()) {
            return this.location;
        } else {
            Location loc = ProtocolLib.getLocationOrNull(this.bukkit);
            return loc != null
                    ? loc
                    : SpartanLocation.bukkitDefault.clone();
        }
    }

    public Location getFromLocation() {
        return this.from;
    }

    public Location getVehicleLocation() {
        Entity vehicle = this.spartan.getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location vehicleLoc = ProtocolLib.getLocationOrNull(vehicle);
            return vehicleLoc;
        } else {
            return null;
        }
    }

    public Location getLocationOrVehicle() {
        Location vehicleLocation = getVehicleLocation();

        if (vehicleLocation == null) {
            return this.location;
        } else {
            return vehicleLocation;
        }
    }

    public SpartanLocation getPastTickRotation() {
        Location l = this.location.clone();
        l.setYaw(this.from.getYaw());
        l.setPitch(this.from.getPitch());
        return new SpartanLocation(l);
    }

    public boolean teleport(Location location) {
        if (this.spartan.getWorld().equals(location.getWorld())) {
            if (SpartanBukkit.isSynchronised()) {
                this.bukkit.leaveVehicle();
            }
            this.spartan.movement.removeLastLiquidTime();
            this.spartan.trackers.removeMany(PlayerTrackers.TrackerFamily.VELOCITY);

            if (MultiVersion.folia) {
                this.bukkit.teleportAsync(location);
            } else {
                SpartanBukkit.transferTask(
                        this,
                        () -> this.bukkit.teleport(location)
                );
            }
            return true;
        } else {
            return false;
        }
    }

    public Location getTeleport() {
        return this.teleport;
    }

    public void setTeleport(Location teleport) {
        this.teleport = teleport;
    }

    public int getPing() {
        return Latency.ping(this.bukkit);
    }

    public UUID getUUID() {
        return ProtocolLib.isTemporary(this.bukkit)
                ? UUID.randomUUID()
                : this.bukkit.getUniqueId();
    }

    public ComponentY getComponentY() {
        return this.componentY;
    }

    public void startSimulationFlag() {
        this.teleport(this.getFromLocation());
        this.simulationStartPoint = this.getFromLocation().clone();
        this.simulationFlag = true;
        this.simulationDelayPerTP = 1;
    }

    public void endSimulationFlag() {
        this.simulationFlag = false;
    }

    public PlayerProfile getProfile() {
        return this.profile;
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
        this.onGroundFrom = this.onGround;
        this.onGround = isOnGround;

        if (this.onGround) {
            this.spartan.movement.resetAirTicks();
        }
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setFromLocation(Location location) {
        this.from = location;
    }

    public void addRawLocation(Location location) {
        this.positionHistory.addLast(location.clone());
        this.positionHistoryLong.addLast(location.clone());
        if (this.positionHistory.size() > 20) {
            this.positionHistory.removeFirst();
        }
        if (this.positionHistoryLong.size() > 50) {
            this.positionHistoryLong.removeFirst();
        }
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
        this.profile.update(this);
    }

    public boolean packetsEnabled() {
        return SpartanBukkit.packetsEnabled() && !this.spartan.isBedrockPlayer();
    }

    public Set<AxisAlignedBB> getAxisMatrixCache() {
        return this.axisMatrixCache;
    }

    public CheckBoundData getCheckBoundData() {
        return this.checkBoundData;
    }

    public void setCheckBoundData(CheckBoundData checkBoundData) {
        this.checkBoundData = checkBoundData;
    }
}