package me.vagdedes.spartan.compatibility.manual.building;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

public class SuperPickaxe {

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.SuperPickaxe.isFunctional()
                && PlayerData.isPickaxeItem(p.getItemInHand().getType());
    }

}
