package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.TeleportData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class SpartanProtocol {

    public final Player player;
    public final SpartanPlayer spartanPlayer;
    private boolean isOnGround, isSprinting, isSneaking, onLoadStatus;
    private Location location;
    private Vector velocity;
    private long lastTransaction;
    private int ping;
    private short transactionID;
    private PlayerProfile profile;
    public List<TeleportData> teleportEngine;
    private boolean mutateTeleport;

    public SpartanProtocol(Player player) {
        this.player = player;
        this.isOnGround = false;
        this.location = player.getLocation();
        this.isSprinting = false;
        this.isSneaking = false;
        this.onLoadStatus = SpartanBukkit.packetsEnabled_Movement();
        this.velocity = new Vector(0, 0, 0);
        this.spartanPlayer = new SpartanPlayer(this);
        this.lastTransaction = Long.MIN_VALUE;
        this.teleportEngine = new LinkedList<>();
        this.mutateTeleport = false;
        this.profile = ResearchEngine.getPlayerProfile(this.spartanPlayer, false);
    }

    public boolean isOnGround() {
        return SpartanBukkit.packetsEnabled_Movement()
                ? this.isOnGround
                : this.player.isOnGround();
    }

    public boolean isSprinting() {
        return SpartanBukkit.packetsEnabled_Movement()
                ? this.isSprinting
                : this.player.isSprinting();
    }

    public boolean isSneaking() {
        return SpartanBukkit.packetsEnabled_Movement()
                ? this.isSneaking
                : this.player.isSneaking();
    }

    public boolean isOnLoadStatus() {
        return this.onLoadStatus;
    }

    public Location getLocation() {
        return SpartanBukkit.packetsEnabled_Movement()
                ? this.location
                : this.player.getLocation();
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public int getPing() {
        return this.ping;
    }
    public boolean isMutateTeleport() { return this.mutateTeleport; }

    public short getTransactionID() {
        return this.transactionID;
    }

    public PlayerProfile getProfile() {
        return this.profile;
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
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

    public void setOnLoadStatus(boolean onLoadStatus) {
        this.onLoadStatus = onLoadStatus;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public void setMutateTeleport(boolean b) { this.mutateTeleport = b; }

    public void setLastTransaction() {
        if (this.lastTransaction != Long.MIN_VALUE) {
            this.ping = (int) (System.currentTimeMillis() - this.lastTransaction);
        }
        this.lastTransaction = System.currentTimeMillis();
    }

    public short increaseTransactionID() {
        this.transactionID++;

        if (this.transactionID > 1500) {
            this.transactionID = 1488;
        }
        return this.transactionID;
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
    }

}
