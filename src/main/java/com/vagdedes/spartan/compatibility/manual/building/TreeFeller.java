package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class TreeFeller {

    public static boolean canCancel(SpartanPlayer p, SpartanBlock b) {
        return (Compatibility.CompatibilityType.TREE_FELLER.isFunctional() || mcMMO.hasTreeFeller(p))
                && BlockUtils.areWoods(b.material);
    }
}
