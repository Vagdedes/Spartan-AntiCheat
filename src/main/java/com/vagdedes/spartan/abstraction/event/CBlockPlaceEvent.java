package com.vagdedes.spartan.abstraction.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class CBlockPlaceEvent implements Cancellable {

    private boolean cancelled;
    public final Block placedBlock, placedAgainstBlock;
    public final Player player;

    public CBlockPlaceEvent(Player player, Block placedBlock, Block placedAgainst, boolean cancelled) {
        this.player = player;
        this.placedAgainstBlock = placedAgainst;
        this.placedBlock = placedBlock;
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
