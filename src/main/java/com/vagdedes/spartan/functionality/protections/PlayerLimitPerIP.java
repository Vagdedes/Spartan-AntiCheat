package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayerLimitPerIP {

    private static class Storage {

        private final String ipAddress;
        private int kickTicks;

        private Storage(String ipAddress) {
            this.ipAddress = ipAddress;
            this.kickTicks = -1;
        }
    }

    private static final Map<Player, Storage> memory = new LinkedHashMap<>(Config.getMaxPlayers());

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                Iterator<Map.Entry<Player, Storage>> iterator = memory.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Player, Storage> entry = iterator.next();
                    Storage storage = entry.getValue();

                    if (storage.kickTicks == 0) {
                        Player p = entry.getKey();

                        if (p.isOnline()) {
                            p.kickPlayer(ConfigUtils.replaceWithSyntax(p, Config.messages.getColorfulString("player_ip_limit_kick_message"), null));
                        }
                        iterator.remove();
                    } else if (storage.kickTicks > 0) {
                        storage.kickTicks -= 1;
                    }
                }
            }, 1L, 1L);
        }
    }

    public static void clear() {
        memory.clear();
    }

    public static String get(Player p) {
        InetSocketAddress ip = p.getAddress();

        if (ip != null) {
            InetAddress address = ip.getAddress();
            return address == null ? null : get(address);
        }
        return null;
    }

    public static String get(InetAddress address) {
        return address.toString().substring(1);
    }

    public static void cache() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                String ip = p.ipAddress;

                if (ip != null) {
                    Player n = p.getPlayer();

                    if (n != null && n.isOnline()) {
                        memory.put(n, new Storage(ip));
                    }
                }
            }
        }
    }

    public static boolean add(Player p) {
        String ip = get(p);

        if (ip != null) {
            int limit = Config.settings.getInteger("Protections.player_limit_per_ip");

            if (limit > 0) {
                int count = 0;

                for (Storage storage : memory.values()) {
                    if (storage.ipAddress.equals(ip)) {
                        count++;
                    }
                }

                if (count >= limit
                        && !Permissions.has(p, Enums.Permission.RECONNECT)) {
                    Storage storage = new Storage(ip);
                    storage.kickTicks = 5;
                    memory.put(p, storage);
                    return true;
                }
            }
            memory.put(p, new Storage(ip));
        }
        return false;
    }

    public static boolean isLimited(Player p) {
        Storage storage = memory.get(p);
        return storage != null && storage.kickTicks >= 0;
    }

    public static void remove(SpartanPlayer p) {
        String ip = p.ipAddress;

        if (ip != null) {
            Player n = p.getPlayer();

            if (n != null && n.isOnline()) {
                memory.remove(n);
            }
        }
    }
}
