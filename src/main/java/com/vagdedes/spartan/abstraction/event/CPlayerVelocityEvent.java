package com.vagdedes.spartan.abstraction.event;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.event.player.PlayerVelocityEvent;

public class CPlayerVelocityEvent {

    /*
    This event is only called when the packet has reached the client.
     */
    private final SpartanProtocol protocol;
    private final PlayerVelocityEvent velocityEvent;

    public CPlayerVelocityEvent(SpartanProtocol protocol, PlayerVelocityEvent velocityEvent) {
        this.protocol = protocol;
        this.velocityEvent = velocityEvent;
    }

    public SpartanProtocol getProtocol() {
        return this.protocol;
    }

    public PlayerVelocityEvent getVelocityEvent() {
        return this.velocityEvent;
    }

    public double getXMotion() {
        return this.velocityEvent.getVelocity().getX();
    }

    public double getYMotion() {
        return this.velocityEvent.getVelocity().getY();
    }

    public double getZMotion() {
        return this.velocityEvent.getVelocity().getY();
    }
}
