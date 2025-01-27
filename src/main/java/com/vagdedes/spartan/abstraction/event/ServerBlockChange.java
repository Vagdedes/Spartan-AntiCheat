package com.vagdedes.spartan.abstraction.event;

import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

public class ServerBlockChange {

    public final BlockPosition position;
    @Setter
    @Getter
    private Material data;
    public int tick;

    public ServerBlockChange(BlockPosition position, Material data) {
        this.position = position;
        this.data = data;
        this.tick = 2;
    }

    public long generateHash() {
        return (this.position.getX() * 2L)
                + (this.position.getY())
                * (this.position.getZ() / 2L);
    }
}
