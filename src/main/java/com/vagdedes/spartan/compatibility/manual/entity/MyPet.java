package com.vagdedes.spartan.compatibility.manual.entity;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import de.Keyle.MyPet.MyPetApi;

public class MyPet {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.MyPet.isFunctional() && MyPetApi.getPlayerManager().isMyPetPlayer(p.getPlayer());
    }
}
