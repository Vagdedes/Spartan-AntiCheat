package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.MaterialUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

public class SpartanBlock {

    public final Material material;
    public final byte data;
    public final int x, z;
    public final short y;
    public final boolean liquid, waterLogged;
    private final SpartanLocation location;

    SpartanBlock(World world, Chunk chunk, Material material, byte data, int x, int y, int z, boolean liquid, boolean waterLogged) {
        this.material = material;
        this.data = data;
        this.x = x;
        this.y = (short) y;
        this.z = z;
        this.liquid = liquid;
        this.waterLogged = waterLogged;
        this.location = new SpartanLocation(world, chunk, this.x, this.y, this.z, 0.0f, 0.0f);
    }

    public SpartanBlock(Block block) {
        this.material = block.getType();
        this.x = block.getX();
        this.y = (short) block.getY();
        this.z = block.getZ();
        this.location = new SpartanLocation(block.getWorld(), null, this.x, this.y, this.z, 0.0f, 0.0f);

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

    public void removeBlockCache() {
        synchronized (SpartanLocation.memory) {
            SpartanLocation.memory.remove(this.location.getIdentifier());
        }
    }

    public World getWorld() {
        return this.location.world;
    }

    public Chunk getChunk() {
        return this.location.getChunk();
    }

    public boolean isLiquidOrWaterLogged(boolean lava) {
        return liquid && (lava || material != MaterialUtils.get("lava")) || waterLogged;
    }

    public boolean isNonWaterLoggedLiquid(boolean lava) {
        return liquid && !waterLogged && (lava || material != MaterialUtils.get("lava"));
    }

    public boolean isLiquid(Material material) {
        return liquid && this.material == material;
    }

    public SpartanLocation getLocation() {
        return this.location;
    }
}
