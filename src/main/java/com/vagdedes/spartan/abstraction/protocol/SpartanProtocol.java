package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.protocol.modules.TeleportData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class SpartanProtocol {

    private final long time;
    public final Player player;
    public final SpartanPlayer spartanPlayer;
    private boolean isOnGround, isOnGroundFrom, isSprinting, isSneaking;
    private int loaded;
    private Location location;
    private Vector velocity;
    private PlayerProfile profile;
    public List<TeleportData> teleportEngine;
    private boolean mutateTeleport;
    public final MCClient mcClient;

    public SpartanProtocol(Player player) {
        this.time = System.currentTimeMillis();
        this.mcClient = new MCClient(player);
        this.player = player;
        this.isOnGround = false;
        this.isOnGroundFrom = false;
        this.location = player.getLocation();
        this.isSprinting = false;
        this.isSneaking = false;
        this.loaded = SpartanBukkit.packetsEnabled() ? 0 : (int) TPS.maximum;
        this.velocity = new Vector(0, 0, 0);
        this.spartanPlayer = new SpartanPlayer(this);
        this.teleportEngine = new LinkedList<>();
        this.mutateTeleport = false;
        this.setProfile(ResearchEngine.getPlayerProfile(this, false));
    }

    public long timePassed() {
        return System.currentTimeMillis() - this.time;
    }

    public boolean isOnGround() {
        return SpartanBukkit.packetsEnabled()
                ? this.isOnGround
                : this.player.isOnGround();
    }

    public boolean isOnGroundFrom() {
        return SpartanBukkit.packetsEnabled()
                ? this.isOnGroundFrom
                : this.player.isOnGround();
    }

    public boolean isSprinting() {
        return SpartanBukkit.packetsEnabled()
                ? this.isSprinting
                : this.player.isSprinting();
    }

    public boolean isSneaking() {
        return SpartanBukkit.packetsEnabled()
                ? this.isSneaking
                : this.player.isSneaking();
    }

    public boolean isLoading() {
        return this.loaded < TPS.maximum;
    }

    public Location getLocation() {
        return SpartanBukkit.packetsEnabled()
                ? this.location
                : this.player.getLocation();
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public int getPing() {
        return Latency.ping(this.player);
    }

    public boolean isMutateTeleport() {
        return this.mutateTeleport;
    }

    public PlayerProfile getProfile() {
        return this.profile;
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
        this.isOnGroundFrom = this.isOnGround;
        this.isOnGround = isOnGround;

        if (this.isOnGround) {
            this.spartanPlayer.movement.resetAirTicks();
        }
    }

    public void setSprinting(boolean isSprinting) {
        this.isSprinting = isSprinting;
    }

    public void setSneaking(boolean isSneaking) {
        this.isSneaking = isSneaking;
    }

    public void load() {
        this.loaded++;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setMutateTeleport(boolean b) {
        this.mutateTeleport = b;
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
    }

}
