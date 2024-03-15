package com.vagdedes.spartan.utils.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;

public class NetworkUtils {

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
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!isNull) {
                    players.remove(SpartanBukkit.getPlayer(player.getUniqueId()));
                }
                if (!players.isEmpty()) { // 1 because we already know we have one player
                    for (SpartanPlayer loopPlayer : players) {
                        if (loopPlayer.isOp()) {
                            player = loopPlayer.getPlayer();
                            break;
                        } else if (Permissions.isStaff(loopPlayer)) {
                            player = loopPlayer.getPlayer();
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
