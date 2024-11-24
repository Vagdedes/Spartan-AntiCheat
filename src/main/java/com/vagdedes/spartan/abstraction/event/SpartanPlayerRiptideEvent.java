package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SpartanPlayerRiptideEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack item;
    private final Vector velocity;

    public SpartanPlayerRiptideEvent(@NotNull Player who, @NotNull ItemStack item, @NotNull Vector velocity) {
        super(who);
        this.item = item;
        this.velocity = velocity;
    }

    /** @deprecated */
    @Deprecated
    public SpartanPlayerRiptideEvent(@NotNull Player who, @NotNull ItemStack item) {
        this(who, item, new Vector());
    }

    public @NotNull ItemStack getItem() {
        return this.item;
    }

    public @NotNull Vector getVelocity() {
        return this.velocity.clone();
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
