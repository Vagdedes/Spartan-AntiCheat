package me.vagdedes.spartan.features.protections;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

public class PlayerLimitPerIP {

    private static final List<String> array = new ArrayList<>();
    private static final Map<Player, Integer> kick = new LinkedHashMap<>(Config.getMaxPlayers());
    private static final int delayInTicks = 5;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                Iterator<Map.Entry<Player, Integer>> iterator = kick.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Player, Integer> entry = iterator.next();
                    int ticks = entry.getValue();

                    if (ticks == 0) {
                        Player p = entry.getKey();
                        p.kickPlayer(ConfigUtils.replaceWithSyntax(p, Messages.get("player_ip_limit_kick_message"), null));
                        iterator.remove();
                    } else {
                        entry.setValue(ticks - 1);
                    }
                }
            }, 1L, 1L);
        }
    }

    public static void clear() {
        array.clear();
        kick.clear();
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

        if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                String ip = p.getIpAddress();

                if (ip != null) {
                    array.add(ip);
                }
            }
        }
    }

    public static boolean add(Player p) {
        if (!isLimited(p)) {
            String ip = get(p);

            if (ip != null) {
                int i = 0;

                for (String ips : array) {
                    if (ip.equals(ips)) {
                        i++;
                    }
                }

                if (i > 0) {
                    int limit = Settings.getInteger("Protections.player_limit_per_ip");

                    if (limit > 0 && i >= limit && !Permissions.has(p, Enums.Permission.RECONNECT)) {
                        kick.put(p, delayInTicks);
                        return true;
                    }
                }
                array.add(ip);
            }
        }
        return false;
    }

    public static boolean isLimited(Player p) {
        return kick.containsKey(p);
    }

    public static void remove(SpartanPlayer p) {
        String ip = p.getIpAddress();

        if (ip != null) {
            array.remove(ip);
        }
    }
}
