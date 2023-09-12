package me.vagdedes.spartan.compatibility.manual.entity;

import de.Keyle.MyPet.MyPetApi;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class MyPet {

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.MyPet.isFunctional() && MyPetApi.getPlayerManager().isMyPetPlayer(p.getPlayer());
    }
}
