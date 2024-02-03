package com.vagdedes.spartan.compatibility.manual.abilities.mcmmo;

import com.gmail.nossr50.api.AbilityAPI;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class BackgroundMcMMO {

    static boolean hasAbility(SpartanPlayer p) {
        if (p != null) {
            try {
                return AbilityAPI.isAnyAbilityEnabled(p.getPlayer());
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    static boolean hasTreeFeller(SpartanPlayer p) {
        if (p != null) {
            try {
                return AbilityAPI.treeFellerEnabled(p.getPlayer());
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
