package me.vagdedes.spartan.compatibility.manual.building;

import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;

public class TreeFeller {

    public static boolean canCancel(SpartanPlayer p, SpartanBlock b) {
        return (Compatibility.CompatibilityType.TreeFeller.isFunctional() || mcMMO.hasTreeFeller(p))
                && BlockUtils.areWoods(b.getType());
    }
}
