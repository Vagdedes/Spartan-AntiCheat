package me.vagdedes.spartan.compatibility.manual.damage;

import com.xdefcon.knockbackmaster.api.KnockbackMasterAPI;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class KnockbackMaster {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.KnockbackMaster.isFunctional()
                && KnockbackMasterAPI.isInComboMode(p.getPlayer());
    }
}
