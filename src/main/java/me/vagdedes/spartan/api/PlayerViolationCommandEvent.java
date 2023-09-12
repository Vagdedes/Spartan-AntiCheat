package me.vagdedes.spartan.api;

import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerViolationCommandEvent extends Event implements Cancellable {

    private final Player p;
    private final HackType h;
    private final HackType[] hs;
    private final String c;
    private boolean cancelled;

    public PlayerViolationCommandEvent(Player player, HackType HackType, HackType[] HackTypes, String command) {
        p = player;
        h = HackType;
        hs = HackTypes;
        c = command;
        cancelled = false;
    }

    public Player getPlayer() {
        return p;
    }

    public HackType getHackType() {
        return h;
    }

    public HackType[] getHackTypes() {
        return hs;
    }

    public String getCommand() {
        return c;
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
