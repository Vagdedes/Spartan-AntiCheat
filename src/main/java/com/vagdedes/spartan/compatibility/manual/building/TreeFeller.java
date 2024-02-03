package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class TreeFeller {

    public static boolean canCancel(SpartanPlayer p, SpartanBlock b) {
        return (Compatibility.CompatibilityType.TreeFeller.isFunctional() || mcMMO.hasTreeFeller(p))
                && BlockUtils.areWoods(b.getType());
    }
}
