package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Player;

public class PlayerStayEvent {

    private final boolean onGround;
    private final Player player;

    public PlayerStayEvent(boolean onGround, Player player) {
        this.onGround = onGround;
        this.player = player;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public Player getPlayer() {
        return player;
    }
}
