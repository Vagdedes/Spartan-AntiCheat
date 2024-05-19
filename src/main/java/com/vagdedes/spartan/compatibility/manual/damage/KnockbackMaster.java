package com.vagdedes.spartan.compatibility.manual.damage;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.xdefcon.knockbackmaster.api.KnockbackMasterAPI;

public class KnockbackMaster {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.KNOCKBACK_MASTER.isFunctional()
                && KnockbackMasterAPI.isInComboMode(p.getInstance());
    }
}
