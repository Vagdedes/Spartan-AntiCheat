package me.vagdedes.spartan.api;

import com.vagdedes.spartan.functionality.moderation.AwarenessNotifications;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerViolationEvent extends Event implements Cancellable {

    private final Player p;
    private final HackType h;
    private final String m;
    private boolean cancelled;

    public PlayerViolationEvent(Player player, HackType HackType, String message) {
        p = player;
        h = HackType;
        m = message;
        cancelled = false;
    }

    public Player getPlayer() {
        return p;
    }

    public HackType getHackType() {
        return h;
    }

    public String getMessage() {
        return m;
    }

    @Deprecated
    public int getViolation() {
        return 0;
    }

    @Deprecated
    public double getViolationIncrease() {
        return 0;
    }

    @Deprecated
    public boolean isFalsePositive() {
        AwarenessNotifications.forcefullySend("The Event API method 'isFalsePositive' has been removed.");
        return false;
    }

    @Deprecated
    public String getCategory() {
        AwarenessNotifications.forcefullySend("The Event API method 'getCategory' has been removed.");
        return null;
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
