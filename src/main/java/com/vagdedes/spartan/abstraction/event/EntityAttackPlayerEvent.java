package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class EntityAttackPlayerEvent implements Cancellable {

    public final Player player;
    public final LivingEntity damager;
    private boolean cancelled;

    public EntityAttackPlayerEvent(Player player, LivingEntity damager, boolean cancelled) {
        this.player = player;
        this.damager = damager;
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
