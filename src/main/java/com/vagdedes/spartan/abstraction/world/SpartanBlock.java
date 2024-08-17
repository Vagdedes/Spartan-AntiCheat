package com.vagdedes.spartan.abstraction.world;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

public class SpartanBlock {

    public final Material material;
    public final boolean liquid, waterLogged;
    public final SpartanLocation location;

    SpartanBlock(SpartanLocation location, Material material, boolean liquid, boolean waterLogged) {
        this.material = material;
        this.liquid = liquid;
        this.waterLogged = waterLogged;
        this.location = location;
    }

    public SpartanBlock(Block block) {
        Location location = block.getLocation();
        this.location = new SpartanLocation(block.getWorld(), location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f);
        this.material = block.getType();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            Object blockData = block.getBlockData();

            if (blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged()) {
                this.liquid = true;
                this.waterLogged = true;
            } else {
                this.liquid = BlockUtils.isLiquid(block);
                this.waterLogged = false;
            }
        } else {
            this.liquid = BlockUtils.isLiquid(block);
            this.waterLogged = false;
        }
    }

    public World getWorld() {
        return this.location.world;
    }

    public int getX() {
        return this.location.getBlockX();
    }

    public int getY() {
        return this.location.getBlockY();
    }

    public int getZ() {
        return this.location.getBlockZ();
    }

    public boolean isLiquidOrWaterLogged(boolean lava) {
        return this.liquid && (lava || this.material != MaterialUtils.get("lava")) || this.waterLogged;
    }

    public boolean isNonWaterLoggedLiquid(boolean lava) {
        return this.liquid && !this.waterLogged && (lava || this.material != MaterialUtils.get("lava"));
    }

    public boolean isLiquid(Material material) {
        return this.liquid && this.material == material;
    }

}
