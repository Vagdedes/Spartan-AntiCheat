package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PlayerAttackEvent {

    private final Player player;
    private final LivingEntity target;
    private final boolean cancelled;

    public PlayerAttackEvent(Player player, LivingEntity target, boolean cancelled) {
        this.player = player;
        this.target = target;
        this.cancelled = cancelled;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Player getPlayer() {
        return player;
    }
}
