package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EntityAttackPlayerEvent {

    private final Player player;
    private final LivingEntity damager;
    private final boolean cancelled;

    public EntityAttackPlayerEvent(Player player, LivingEntity damager, boolean cancelled) {
        this.player = player;
        this.damager = damager;
        this.cancelled = cancelled;
    }

    public LivingEntity getDamager() {
        return damager;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Player getPlayer() {
        return player;
    }
}
