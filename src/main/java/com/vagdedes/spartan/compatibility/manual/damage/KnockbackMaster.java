package com.vagdedes.spartan.compatibility.manual.damage;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.xdefcon.knockbackmaster.api.KnockbackMasterAPI;

public class KnockbackMaster {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.KnockbackMaster.isFunctional()
                && KnockbackMasterAPI.isInComboMode(p.getPlayer());
    }
}
