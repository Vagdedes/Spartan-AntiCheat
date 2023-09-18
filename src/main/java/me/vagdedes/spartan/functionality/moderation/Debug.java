package me.vagdedes.spartan.functionality.moderation;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.util.*;

public class Debug {

    private static final Map<UUID, Map<UUID, Collection<Enums.Debug>>> hm = new LinkedHashMap<>(Config.getMaxPlayers());

    public static void clear() {
        hm.clear();
    }

    public static boolean has(SpartanPlayer p, SpartanPlayer t, Enums.Debug debug) {
        Map<UUID, Collection<Enums.Debug>> map = hm.get(p.getUniqueId());

        if (map != null) {
            Collection<Enums.Debug> data = map.get(t.getUniqueId());
            return data != null && data.contains(debug);
        }
        return false;
    }

    public static boolean has(SpartanPlayer p, SpartanPlayer t) {
        Map<UUID, Collection<Enums.Debug>> map = hm.get(p.getUniqueId());
        return map != null && map.containsKey(t.getUniqueId());
    }

    public static void remove(SpartanPlayer p, SpartanPlayer t) {
        UUID uuid = p.getUniqueId(),
                target = t.getUniqueId();
        Map<UUID, Collection<Enums.Debug>> map = hm.get(uuid);

        if (map != null && map.remove(target) != null) {
            if (map.isEmpty()) {
                hm.remove(uuid);
            }
            String msg = Config.messages.getColorfulString("debug_disable_all_message").replace("{player}", t.getName());
            p.getPlayer().sendMessage(msg);
        }
    }

    public static void add(SpartanPlayer p, SpartanPlayer t, Enums.Debug s) {
        boolean enable;
        UUID uuid = p.getUniqueId(),
                target = t.getUniqueId();
        Map<UUID, Collection<Enums.Debug>> map = hm.get(uuid);

        if (map == null) {
            enable = true;
            map = new LinkedHashMap<>();
            Collection<Enums.Debug> data = new HashSet<>(Enums.Debug.values().length);
            data.add(s);
            map.put(target, data);
            hm.put(uuid, map);
        } else {
            Collection<Enums.Debug> data = map.get(target);

            if (data != null) {
                if (data.add(s)) {
                    enable = true;
                } else {
                    enable = false;

                    if (data.remove(s) && data.isEmpty()) {
                        map.remove(target);
                    }
                }
            } else {
                enable = true;
                data = new HashSet<>(Enums.Debug.values().length);
                data.add(s);
                map.put(target, data);
            }
        }
        String msg = Config.messages.getColorfulString(enable ? "debug_enable_message" : "debug_disable_message");
        msg = msg.replace("{player}", t.getName());
        msg = msg.replace("{type}", s.getString());
        p.getPlayer().sendMessage(msg);
    }

    public static void inform(SpartanPlayer p, Enums.Debug t, String s) {
        for (Map.Entry<UUID, Map<UUID, Collection<Enums.Debug>>> entry : hm.entrySet()) {
            Collection<Enums.Debug> data = entry.getValue().get(p.getUniqueId());

            if (data != null
                    && data.contains(t)) {
                SpartanPlayer owner = SpartanBukkit.getPlayer(entry.getKey());

                if (owner != null) {
                    String msg = Config.messages.getColorfulString("debug_player_message");
                    msg = msg.replace("{player}", p.getName());
                    msg = msg.replace("{type}", t.getString());
                    msg = msg.replace("{info}", s.toLowerCase().replace("_", "-"));
                    owner.sendMessage(msg);
                }
            }
        }
    }

    public static boolean canRun() {
        return !hm.isEmpty();
    }
}
