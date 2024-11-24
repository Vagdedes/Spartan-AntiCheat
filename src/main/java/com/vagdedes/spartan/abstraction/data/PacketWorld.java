package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.ServerBlockChange;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class PacketWorld {
    private List<ServerBlockChange> query;
    private final Player player;
    private int lagTick;

    public PacketWorld(Player player) {
        this.player = player;
        this.query = new ArrayList<>();
        this.lagTick = 0;
    }


    public void tick(PlayerTickEvent tickEvent) {
        if (tickEvent.getDelay() < 12) {
            this.lagTick = 2;
        } else if (lagTick > 0) {
            this.lagTick--;
        } else {
            List<ServerBlockChange> toDelete = new ArrayList<>();
            for (ServerBlockChange change : this.query) {
                change.tick--;
                if (change.tick == 0) {
                    toDelete.add(change);
                }
            }
            for (ServerBlockChange change : toDelete) {
                this.query.remove(change);
            }
            toDelete.clear();
        }
    }
    public List<ServerBlockChange> getLocalWorld() {
        return this.query;
    }
    public Material getBlock(Location location) {
        SpartanProtocol p = SpartanBukkit.getProtocol(this.player);
        for (ServerBlockChange change : this.query) {
            Location lL = change.getPosition().toLocation(this.player.getWorld());

            if (Math.abs(lL.getX() - location.getX()) <= 1.0 &&
                            Math.abs(lL.getY() - location.getY()) <= 1.0 &&
                            Math.abs(lL.getZ() - location.getZ()) <= 1.0) {
                return change.getData();
            }
        }

        Block b = this.getBlockAsync(location);
        return (b == null) ? null : b.getType();
    }

    public void worldChange(ServerBlockChange blockChange) {
        if (BlockUtils.areAir(blockChange.getData())) {
            Block b = this.getBlockAsync(blockChange
                            .getPosition()
                            .toLocation(this.player.getWorld()));
            if (b == null) return;
            blockChange.setData(b.getType());
        }
        long hash = blockChange.hashCode();
        ServerBlockChange toDelete = null;
        for (ServerBlockChange c : this.query) {
            if (c.generateHash() == hash) {
                toDelete = c;
                break;
            }
        }
        if (toDelete != null)
            this.query.remove(toDelete);
        this.query.add(blockChange);
    }

    private Block getBlockAsync(final Location location) {
        if (Event_Chunks.isLoaded(
                        location.getWorld(),
                        location.getBlockX() >> 4,
                        location.getBlockZ() >> 4
        )) {
            return location.getWorld().getBlockAt(location);
        } else {
            return null;
        }
    }

}
