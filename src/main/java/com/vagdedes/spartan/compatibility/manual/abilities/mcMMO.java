package com.vagdedes.spartan.compatibility.manual.abilities;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.util.player.UserManager;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import org.bukkit.entity.Player;

public class mcMMO {

    public static boolean hasGeneralAbility(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
            McMMOPlayer n = getMcMMOPlayer(p);

            if (n != null) {
                for (SuperAbilityType type : SuperAbilityType.values()) {
                    if (n.getAbilityMode(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasTreeFeller(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
            McMMOPlayer n = getMcMMOPlayer(p);
            return n != null && n.getAbilityMode(SuperAbilityType.TREE_FELLER);
        } else {
            return false;
        }
    }

    private static McMMOPlayer getMcMMOPlayer(SpartanPlayer p) {
        Player n = p.getInstance();
        return n == null ? null : UserManager.getPlayer(n);
    }

}
