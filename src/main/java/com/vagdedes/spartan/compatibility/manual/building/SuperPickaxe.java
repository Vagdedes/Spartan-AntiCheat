package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerData;

public class SuperPickaxe {

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.SuperPickaxe.isFunctional()
                && PlayerData.isPickaxeItem(p.getItemInHand().getType());
    }

}
