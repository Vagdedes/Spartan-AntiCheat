package com.vagdedes.spartan.utils.minecraft.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProxyUtils {

    private static final String name = "BungeeCord";

    public static void register() {
        if (!Bukkit.getOnlineMode()) {
            Register.plugin.getServer().getMessenger().registerOutgoingPluginChannel(Register.plugin, name);
        }
    }

    public static void unregister() {
        Register.plugin.getServer().getMessenger().unregisterIncomingPluginChannel(Register.plugin);
    }

    public static boolean executeCommand(Player player, String command) {
        if (!Bukkit.getOnlineMode()) {
            boolean isNull = player == null;

            if (isNull || !player.isOp()) {
                Set<Map.Entry<UUID, SpartanProtocol>> entries = SpartanBukkit.getPlayerEntries();

                if (!isNull) {
                    Iterator<Map.Entry<UUID, SpartanProtocol>> iterator = entries.iterator();

                    while (iterator.hasNext()) {
                        UUID uuid = iterator.next().getKey();

                        if (player.getUniqueId().equals(uuid)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
                if (!entries.isEmpty()) { // 1 because we already know we have one player
                    for (Map.Entry<UUID, SpartanProtocol> entry : entries) {
                        SpartanPlayer loopPlayer = entry.getValue().spartanPlayer;

                        if (loopPlayer.getInstance().isOp()) {
                            player = loopPlayer.getInstance();
                            break;
                        } else if (Permissions.isStaff(loopPlayer)) {
                            player = loopPlayer.getInstance();
                        }
                    }
                }
            }
            if (player != null) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);

                try {
                    out.writeUTF("Message");
                    out.writeUTF("ALL");
                    out.writeUTF("/" + command);
                    player.sendPluginMessage(Register.plugin, name, b.toByteArray());
                    return true;
                } catch (Exception ex) {
                    AwarenessNotifications.forcefullySend(
                            name + " Command Failed: "
                                    + "\nPlayer: " + player.getName()
                                    + "\nCommand: " + command
                    );
                    ex.printStackTrace();
                }
            } else {
                AwarenessNotifications.forcefullySend(
                        name + " Command Failed: "
                                + "\nPlayer: NULL"
                                + "\nCommand: " + command
                );
            }
        } else {
            AwarenessNotifications.forcefullySend(
                    name + " Command Failed: "
                            + "\nServer: Not-Proxy-Supported"
                            + "\nCommand: " + command
            );
        }
        return false;
    }
}
