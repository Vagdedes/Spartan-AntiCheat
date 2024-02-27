package com.vagdedes.spartan.objects.replicates;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.Chunks;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

public class SpartanBlock {

    public final SpartanPlayer player;
    public final Material material;
    public final World world;
    private Chunk chunk;
    public final byte data;
    public final int x, z, identifier;
    public final short y;
    public final boolean liquid, waterLogged;

    SpartanBlock(SpartanPlayer player, World world, Chunk chunk, Material material, byte data, int identifier, int x, int y, int z, boolean liquid, boolean waterLogged) {
        this.player = player;
        this.world = world;
        this.material = material;
        this.chunk = chunk;
        this.data = data;
        this.x = x;
        this.y = (short) y;
        this.z = z;
        this.liquid = liquid;
        this.waterLogged = waterLogged;
        this.identifier = identifier;
    }

    public SpartanBlock(SpartanPlayer player, Block block) {
        this.player = player;
        this.world = block.getWorld();
        this.material = block.getType();
        this.chunk = null;
        this.x = block.getX();
        this.y = (short) block.getY();
        this.z = block.getZ();
        this.identifier = Chunks.locationIdentifier(world.hashCode(), x, y, z);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            Object blockData = block.getBlockData();

            if (blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged()) {
                this.liquid = true;
                this.waterLogged = true;
            } else {
                this.liquid = BlockUtils.isLiquid(block);
                this.waterLogged = false;
            }
            this.data = (byte) (blockData instanceof Levelled ? ((Levelled) blockData).getLevel() : 0);
        } else {
            this.liquid = BlockUtils.isLiquid(block);
            this.waterLogged = false;
            this.data = block.getData();
        }
    }

    public Chunk getChunk() {
        if (chunk == null) {
            chunk = getLocation().getChunk();
        }
        return chunk;
    }

    public boolean isLiquid() {
        return liquid || waterLogged;
    }

    public boolean isNonWaterLoggedLiquid() {
        return liquid && !waterLogged;
    }

    public SpartanLocation getLocation() {
        return new SpartanLocation(this.player, this.world, this.chunk, this.x, this.y, this.z, 0.0f, 0.0f, this);
    }

    public Block getBlock() {
        SpartanLocation loc = getLocation();
        return loc.getWorld().getBlockAt(loc.getLimitedBukkitLocation());
    }
}
