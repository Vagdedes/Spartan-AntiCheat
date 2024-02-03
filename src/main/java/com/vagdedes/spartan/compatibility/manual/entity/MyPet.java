package com.vagdedes.spartan.compatibility.manual.entity;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import de.Keyle.MyPet.MyPetApi;

public class MyPet {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.MyPet.isFunctional() && MyPetApi.getPlayerManager().isMyPetPlayer(p.getPlayer());
    }
}
