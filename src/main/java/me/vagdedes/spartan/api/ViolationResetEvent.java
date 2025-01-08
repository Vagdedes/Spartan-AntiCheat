package me.vagdedes.spartan.api;

import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Deprecated
public class ViolationResetEvent extends Event implements Cancellable {

    @Deprecated
    public ViolationResetEvent(Player player, Enums.HackType hackType) {
    }

    @Deprecated
    public Player getPlayer() {
        return null;
    }

    @Deprecated
    public Enums.HackType getHackType() {
        return null;
    }

    @Deprecated
    public int getTime() {
        return BackgroundAPI.getViolationResetTime();
    }

    @Deprecated
    public boolean isCancelled() {
        return false;
    }

    @Deprecated
    public void setCancelled(boolean b) {
    }

    @Deprecated
    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
