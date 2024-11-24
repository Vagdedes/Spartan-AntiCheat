package com.vagdedes.spartan.abstraction.world;

import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class SpartanBlock {

    private final Object block;

    SpartanBlock(Object block) {
        this.block = block;
    }

    public boolean dataLoaded() {
        return this.block != null;
    }

    public boolean isMaterial() {
        return this.block instanceof Material;
    }

    public boolean isBlock() {
        return this.block instanceof Block;
    }

    public boolean isBlockData() {
        return BlockUtils.blockDataExists
                && this.block instanceof BlockData;
    }

    public Material getType() {
        if (this.block instanceof Block) {
            return ((Block) this.block).getType();
        } else if (this.block instanceof Material) {
            return (Material) this.block;
        } else if (BlockUtils.blockDataExists
                && this.block instanceof BlockData) {
            return ((BlockData) this.block).getMaterial();
        } else {
            return Material.AIR;
        }
    }

    public Material getTypeOrNull() {
        if (this.block instanceof Block) {
            return ((Block) this.block).getType();
        } else if (this.block instanceof Material) {
            return (Material) this.block;
        } else if (BlockUtils.blockDataExists
                && this.block instanceof BlockData) {
            return ((BlockData) this.block).getMaterial();
        } else {
            return null;
        }
    }

    public boolean isWaterLogged() {
        return this.block instanceof Block
                ? BlockUtils.isWaterLogged((Block) this.block)
                : BlockUtils.blockDataExists
                && this.block instanceof BlockData
                && BlockUtils.isWaterLogged((BlockData) this.block);
    }

    public boolean isLiquidOrWaterLogged(boolean lava) {
        return this.block instanceof Block
                ? BlockUtils.isLiquidOrWaterLogged((Block) this.block, lava)
                : this.block instanceof Material
                ? BlockUtils.isLiquid((Material) this.block)
                : BlockUtils.blockDataExists
                && this.block instanceof BlockData
                && BlockUtils.isLiquidOrWaterLogged((BlockData) this.block, lava);
    }

    public boolean isLiquid(Material target) {
        if (this.block instanceof Block) {
            Block block = (Block) this.block;
            return BlockUtils.isLiquid(block) && block.getType() == target;
        } else {
            Material material;

            if (this.block instanceof Material) {
                material = (Material) this.block;
            } else if (BlockUtils.blockDataExists
                    && this.block instanceof BlockData) {
                material = ((BlockData) this.block).getMaterial();
            } else {
                return false;
            }
            return BlockUtils.isLiquid(material) && material == target;
        }
    }

}
