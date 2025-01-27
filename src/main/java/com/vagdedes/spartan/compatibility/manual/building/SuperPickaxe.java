package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.protocol.PlayerBukkit;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;

public class SuperPickaxe {

    public static boolean isUsing(PlayerBukkit p) {
        return Compatibility.CompatibilityType.SUPER_PICKAXE.isFunctional()
                && PlayerUtils.isPickaxeItem(p.getItemInHand().getType());
    }

}
