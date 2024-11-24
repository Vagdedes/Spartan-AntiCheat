package com.vagdedes.spartan.abstraction.event;

import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Material;

public class ServerBlockChange {
    private final BlockPosition position;
    private Material data;
    public int tick;
    public ServerBlockChange(BlockPosition position, Material data) {
        this.position = position;
        this.data = data;
        this.tick = 2;
    }
    public BlockPosition getPosition() {
        return this.position;
    }
    public Material getData() {
        return this.data;
    }
    public void setData(Material data) {
        this.data = data;
    }
    public long generateHash() {
        return (this.position.getX() * 2L)
                + (this.position.getY())
                        * (this.position.getZ() / 2L);
    }
}
