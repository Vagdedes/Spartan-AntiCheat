package me.vagdedes.spartan.handlers.identifiers.complex.predictable;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class Liquid {

    public static void runInteract(SpartanPlayer p, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK && !p.getProfile().isHacker()) {
            ItemStack itemStack = p.getItemInHand();

            if (itemStack != null
                    && itemStack.getType() == Material.WATER_BUCKET) {
                add(p);
            }
        }
    }

    public static boolean runMove(SpartanPlayer p) {
        if (p.isSwimming() || isLocation(p, p.getLocation())) {
            add(p);
            return true;
        }
        return false;
    }

    public static void remove(SpartanPlayer p) {
        p.removeLastLiquidTime();
        WaterElevator.remove(p);
    }

    private static void add(SpartanPlayer p) {
        p.setLastLiquidTime();

        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && Compatibility.CompatibilityType.ViaVersion.isFunctional()
                && Math.round(p.getEyeHeight() * 10.0) == 4.0) {
            p.setSwimming(false, 10);
        }
        WaterElevator.runMove(p);
    }

    // Utils

    public static boolean isLocation(SpartanPlayer player, SpartanLocation location) {
        double hitbox = player.isBedrockPlayer() ? BlockUtils.hitbox_max : BlockUtils.hitbox;
        return location.getBlock().isLiquid()
                || PatternUtils.isLiquidPattern(new double[][]{
                {hitbox, 0, hitbox},
                {hitbox, 1, hitbox},
                {hitbox, player.getEyeHeight() + MoveUtils.lowPrecision, hitbox}
        }, location, true);
    }
}
