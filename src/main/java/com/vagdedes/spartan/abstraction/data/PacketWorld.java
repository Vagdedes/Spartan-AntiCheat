package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.ServerBlockChange;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_Chunks;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PacketWorld {

    private final List<ServerBlockChange> query;
    private final Player player;
    private int lagTick;
    public boolean transactionLock;

    public PacketWorld(Player player) {
        this.player = player;
        this.query = Collections.synchronizedList(new ArrayList<>());
        this.lagTick = 0;
    }


    public void tick(PlayerTickEvent tickEvent) {
        if (tickEvent.getDelay() < 12) {
            this.lagTick = 3;
        } else if (lagTick > 0) {
           if (!this.transactionLock) this.lagTick--;
        } else {
            synchronized (this.query) {
                this.query.removeIf(change -> --change.tick == 0);
            }
        }
    }

    public List<ServerBlockChange> getLocalWorld() {
        synchronized (this.query) {
            return new ArrayList<>(this.query);
        }
    }

    public Material getBlock(Location location) {
        SpartanProtocol p = SpartanBukkit.getProtocol(this.player);

        synchronized (this.query) {
            for (ServerBlockChange change : this.query) {
                Location lL = change.getPosition().toLocation(this.player.getWorld());

                if (Math.abs(lL.getX() - location.getX()) <= 1.0 &&
                                Math.abs(lL.getY() - location.getY()) <= 1.0 &&
                                Math.abs(lL.getZ() - location.getZ()) <= 1.0) {
                    return change.getData();
                }
            }
        }

        Block b = Event_Chunks.getBlockAsync(location);
        return (b == null) ? null : b.getType();
    }

    public void worldChange(ServerBlockChange blockChange) {
        if (BlockUtils.areAir(blockChange.getData())) {
            Block b = Event_Chunks.getBlockAsync(
                            blockChange
                                            .getPosition()
                                            .toLocation(this.player.getWorld())
            );
            if (b == null || BlockUtils.areAir(b.getType())) return;
            blockChange.setData(b.getType());
        }
        long hash = blockChange.hashCode();
        ServerBlockChange toDelete = null;
        synchronized (this.query) {
            for (ServerBlockChange c : this.query) {
                if (c.generateHash() == hash) {
                    toDelete = c;
                    break;
                }
            }
            if (toDelete != null) {
                this.query.remove(toDelete);
            }
            this.query.add(blockChange);
            this.transactionLock = true;
            this.lagTick = 3;
        }
    }

}