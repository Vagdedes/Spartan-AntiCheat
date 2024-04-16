package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class SuperPickaxe {

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.SuperPickaxe.isFunctional()
                && PlayerUtils.isPickaxeItem(p.getItemInHand().getType());
    }

}
