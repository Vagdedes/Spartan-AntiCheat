package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.concurrent.CheckThread;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UseItemStatusHandle extends PacketAdapter {

    public UseItemStatusHandle() {
        super(Register.plugin, ListenerPriority.NORMAL, resolvePacketTypes());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(player);
        PacketContainer packet = event.getPacket();
        CheckThread.run(() -> {
            //player.sendMessage("p: " + event.getPacket().getType() + " " + event.getPacket().getStructures().getValues());
            if (packet.getType().equals(PacketType.Play.Client.BLOCK_DIG)) {
                protocol.useItemPacket = false;
            } else {
                BlockPosition blockPosition = new BlockPosition(0, 0, 0);
                if (packet.getHands().getValues().isEmpty()) {
                    if (!packet.getMovingBlockPositions().getValues().isEmpty()) {
                        blockPosition = packet.getMovingBlockPositions().read(0).getBlockPosition();
                    }
                    if (!packet.getBlockPositionModifier().getValues().isEmpty()) {
                        blockPosition = packet.getBlockPositionModifier().read(0);
                    }
                    if (blockPosition.getY() != -1) return;
                }
                boolean isMainHand = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                        || event.getPacket().getHands().read(0) == EnumWrappers.Hand.MAIN_HAND;
                ItemStack itemStack = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                        ? (isMainHand ? player.getInventory().getItemInMainHand()
                        : player.getInventory().getItemInOffHand()) : player.getItemInHand();
                if (packet.getType().toString().contains("USE_ITEM_ON")) {
                    if (event.getPacket().getStructures().getValues().toString().contains("Serverbound"))
                        return;
                }
                if (itemStack.getType().toString().contains("SHIELD") ||
                        (itemStack.getType().isEdible() && (player.getFoodLevel() != 20
                                || itemStack.getType().toString().contains("GOLDEN_APPLE"))
                                && !player.getGameMode().equals(GameMode.CREATIVE))) {
                    //player.sendMessage("use");
                    protocol.useItemPacket = true;
                    protocol.useItemPacketReset =
                                    !(itemStack.getType().toString().contains("SHIELD") ||
                                    itemStack.getType().toString().contains("GOLDEN_APPLE"));
                }
            }
        });
    }

    private static PacketType[] resolvePacketTypes() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return new PacketType[]{
                    PacketType.Play.Client.USE_ITEM,
                    PacketType.Play.Client.BLOCK_DIG
            };
        } else {
            if (ProtocolLib.isPacketSupported("USE_ITEM_ON")) {
                return new PacketType[]{
                        PacketType.Play.Client.USE_ITEM_ON,
                        PacketType.Play.Client.BLOCK_PLACE,
                        PacketType.Play.Client.BLOCK_DIG
                };
            } else {
                return new PacketType[]{
                        PacketType.Play.Client.BLOCK_PLACE,
                        PacketType.Play.Client.BLOCK_DIG
                };
            }
        }
    }


}