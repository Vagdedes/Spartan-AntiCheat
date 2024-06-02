package com.vagdedes.spartan.compatibility.manual.entity;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import de.Keyle.MyPet.MyPetApi;

public class MyPet {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.MY_PET.isFunctional() && MyPetApi.getPlayerManager().isMyPetPlayer(p.getInstance());
    }
}
