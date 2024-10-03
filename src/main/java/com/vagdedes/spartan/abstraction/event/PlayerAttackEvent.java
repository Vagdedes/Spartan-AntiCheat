package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PlayerAttackEvent {

    public final Player player;
    public final LivingEntity target;
    public final boolean cancelled;

    public PlayerAttackEvent(Player player, LivingEntity target, boolean cancelled) {
        this.player = player;
        this.target = target;
        this.cancelled = cancelled;
    }

}
