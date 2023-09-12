package me.vagdedes.spartan.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerFoundOreEvent extends Event implements Cancellable {

    private final Player p;
    private final String me;
    private final Location l;
    private final Material ma;
    private boolean cancelled;

    public PlayerFoundOreEvent(Player player, String message, Location location, Material material) {
        p = player;
        me = message;
        l = location;
        ma = material;
        cancelled = false;
    }

    public Player getPlayer() {
        return p;
    }

    public String getMessage() {
        return me;
    }

    public Location getLocation() {
        return l;
    }

    public Material getMaterial() {
        return ma;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
