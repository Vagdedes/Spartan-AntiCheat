package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EntityAttackPlayerEvent {

    public final Player player;
    public final LivingEntity damager;
    public final boolean cancelled;

    public EntityAttackPlayerEvent(Player player, LivingEntity damager, boolean cancelled) {
        this.player = player;
        this.damager = damager;
        this.cancelled = cancelled;
    }

}
