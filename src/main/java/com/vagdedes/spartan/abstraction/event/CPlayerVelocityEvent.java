package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CPlayerVelocityEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel = false;
    private Vector velocity;

    public CPlayerVelocityEvent(@NotNull Player player, @NotNull Vector velocity) {
        super(player);
        this.velocity = velocity;
    }

    public boolean isCancelled() {
        return this.cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public @NotNull Vector getVelocity() {
        return this.velocity;
    }

    public void setVelocity(@NotNull Vector velocity) {
        this.velocity = velocity;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
