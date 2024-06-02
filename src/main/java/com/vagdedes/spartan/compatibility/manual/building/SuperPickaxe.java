package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;

public class SuperPickaxe {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.SUPER_PICKAXE.isFunctional()
                && PlayerUtils.isPickaxeItem(p.getItemInHand().getType());
    }

}
