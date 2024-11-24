package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PlayerUseEvent {

    private final Player player;
    private final LivingEntity target;
    private final boolean cancelled;

    public PlayerUseEvent(Player player, LivingEntity target, boolean cancelled) {
        this.player = player;
        this.target = target;
        this.cancelled = cancelled;
    }

    public Player getPlayer() {
        return this.player;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}