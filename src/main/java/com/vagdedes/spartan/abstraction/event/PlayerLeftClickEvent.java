package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Player;

public class PlayerLeftClickEvent {

    public final Player player;
    public final long delay;

    public PlayerLeftClickEvent(Player player, long delay) {
        this.player = player;
        this.delay = delay;
    }

}
