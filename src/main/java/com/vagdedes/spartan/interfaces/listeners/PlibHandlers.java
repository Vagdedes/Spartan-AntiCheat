package com.vagdedes.spartan.interfaces.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.checks.movement.NoSlowdown;
import com.vagdedes.spartan.compatibility.manual.essential.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;

public class PlibHandlers {

    public static void runNoSlowdown() {
        String key = "packets";

        try {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Register.plugin, PacketType.Play.Client.USE_ITEM) {
                @Override
                public void onPacketReceiving(final PacketEvent e) {
                    if (!e.isCancelled() && (!ProtocolLib.canHaveTemporaryPlayers() || !e.isPlayerTemporary())
                            && NoSlowdown.check.getCheck().getBooleanOption("check_packets", true)) {
                        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

                        if (p == null
                                || p.getProfile().playerCombat.isActivelyFighting(null, true, true, false)) {
                            return;
                        }

                        // Speed Effect
                        SpartanLocation loc = p.getLocation();

                        if (PlayerData.hasSpeedEffect(p, loc)) {
                            return;
                        }

                        // Ice Blocks
                        Cooldowns cooldowns = p.getCooldowns(NoSlowdown.check);

                        if (BlockUtils.areIceBlocks(loc) || BlockUtils.areIceBlocks(loc.clone().add(0, -1, 0))) {
                            cooldowns.add(key + "=ice-blocks", 40);
                            return;
                        }

                        // Cancellation Handler
                        if (!p.isOnGroundCustom() || p.getTicksOnAir() > 0
                                || !cooldowns.canDo(key + "=ice-blocks")
                                || !cooldowns.canDo(key + "=cooldown")) {
                            return;
                        }
                        // Fields
                        int delay = 5, maxDelay = 8;
                        Material type = p.getItemInHand().getType();

                        // Items
                        boolean sword = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && CombatUtils.isSword(type),
                                trident = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && type == Material.TRIDENT,
                                food = type.isEdible(),
                                bow = (type == Material.BOW || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && type == Material.CROSSBOW) && p.getInventory().contains(Material.ARROW),
                                potion = type == Material.POTION,
                                shield = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && PlayerData.hasItemInHands(p, Material.SHIELD);

                        // Detection
                        if (sword || trident || food || bow || potion || shield) {
                            cooldowns.add(key + "=cooldown", sword ? (delay = maxDelay) : delay);

                            SpartanBukkit.runDelayedTask(p, () -> {
                                if (p != null && cooldowns.canDo(key + "=failure") && p.getLastOffSprint() >= 500L) {
                                    Material newType = p.getItemInHand().getType();

                                    if (sword && CombatUtils.isSword(type)
                                            || trident && newType == Material.TRIDENT
                                            || food && newType.isEdible()
                                            || bow && (newType == Material.BOW || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && newType == Material.CROSSBOW) && p.getInventory().contains(Material.ARROW)
                                            || potion && newType == Material.POTION
                                            || shield && PlayerData.hasItemInHands(p, Material.SHIELD)) {
                                        p.getExecutor(Enums.HackType.NoSlowdown).handle(false, NoSlowdown.PACKETS);
                                    }
                                }
                            }, delay);
                        } else {
                            cooldowns.add(key + "=failure", maxDelay);
                        }
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }
}
