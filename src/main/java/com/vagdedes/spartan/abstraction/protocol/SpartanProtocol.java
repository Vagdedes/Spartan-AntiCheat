package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpartanProtocol {

    public final Player player;
    public final SpartanPlayer spartanPlayer;
    public final AbilitiesContainer abilities;
    public final RotationData lastRotation;
    public Boolean
            onGround,
            spawnStatus,
            canCheck;
    public Location
            position,
            verifiedPosition,
            lastTeleport;
    private final long creationTime;

    public SpartanProtocol(Player player) {
        this.creationTime = System.currentTimeMillis();
        this.player = player;
        this.abilities = new AbilitiesContainer(false, false, false);
        this.lastRotation = new RotationData();
        this.spartanPlayer = new SpartanPlayer(this, player);
    }

    // Utilities

    public boolean hasDataFor(Object object) {
        return object != null
                && (!(object instanceof SpartanProtocolField)
                || ((SpartanProtocolField) object).hasData());
    }

    public boolean trueOrFalse(Boolean bool) {
        return bool != null && bool;
    }

    // Implementations

    public long timePassed() {
        return System.currentTimeMillis() - creationTime;
    }

    public Location getLocation() {
        if (hasDataFor(position)) {
            if (hasDataFor(lastRotation)) {
                position.setYaw(lastRotation.getYaw());
                position.setPitch(lastRotation.getPitch());
            }
            return position;
        } else {
            return player.getLocation();
        }
    }

}
