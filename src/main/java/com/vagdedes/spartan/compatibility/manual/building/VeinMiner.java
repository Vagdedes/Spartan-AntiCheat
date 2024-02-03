package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import wtf.choco.veinminer.api.event.player.PlayerVeinMineEvent;

public class VeinMiner implements Listener {

    public static void reload() {
        Register.enable(new VeinMiner(), VeinMiner.class);
    }

    static void cancel(Player player) {
        CheckProtection.evadeCommonFalsePositives(player, Compatibility.CompatibilityType.VeinMiner,
                new Enums.HackType[]{
                        Enums.HackType.NoSwing,
                        Enums.HackType.FastBreak,
                        Enums.HackType.GhostHand,
                        Enums.HackType.BlockReach
                }, 30);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerVeinMineEvent e) {
        if (Compatibility.CompatibilityType.VeinMiner.isFunctional()) {
            VeinMiner.cancel(e.getPlayer());
        }
    }
}
