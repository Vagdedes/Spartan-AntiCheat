package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.block.Block;

public class TreeFeller {

    public static boolean canCancel(Block b) {
        return Compatibility.CompatibilityType.TREE_FELLER.isFunctional()
                && BlockUtils.areWoods(b.getType());
    }
}
