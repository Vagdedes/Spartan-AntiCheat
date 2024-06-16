package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Permissions {

    private static final String alternativeAdminKey = "spartan.*";
    private static final Enums.Permission[] staffPermissions = new Enums.Permission[]{
            Enums.Permission.STAFF_CHAT, Enums.Permission.WAVE, Enums.Permission.WARN,
            Enums.Permission.ADMIN, Enums.Permission.KICK, Enums.Permission.NOTIFICATIONS,
            Enums.Permission.USE_BYPASS, Enums.Permission.MANAGE, Enums.Permission.INFO,
    };

    // Separator

    public static boolean has(Player p) {
        for (Enums.Permission permission : Enums.Permission.values()) {
            if (has(p, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean has(SpartanPlayer p) {
        Player n = p.getInstance();

        if (n != null) {
            for (Enums.Permission permission : Enums.Permission.values()) {
                if (has(n, permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Separator

    public static boolean has(SpartanPlayer p, Enums.Permission permission) {
        Player n = p.getInstance();
        return n != null && has(n, permission);
    }

    public static boolean has(Player p, Enums.Permission permission) {
        if (p.hasPermission(permission.getKey())) {
            return true;
        } else {
            Enums.Permission admin = Enums.Permission.ADMIN;
            return permission != admin
                    ? p.hasPermission(Enums.Permission.ADMIN.getKey())
                    || p.hasPermission(alternativeAdminKey)
                    : p.hasPermission(alternativeAdminKey);
        }
    }

    // Separator

    public static boolean onlyHas(SpartanPlayer p, Enums.Permission permission) {
        Player n = p.getInstance();
        return n != null && onlyHas(n, permission);
    }

    public static boolean onlyHas(Player p, Enums.Permission permission) {
        return p.hasPermission(permission.getKey());
    }

    // Separator

    public static boolean isBypassing(SpartanPlayer p, Enums.HackType hackType) {
        if (p.getInstance().isOp()) {
            return Config.settings.getBoolean("Important.op_bypass");
        } else {
            Player n = p.getInstance();
            return n != null
                    && (isBypassing(n, null)
                    || hackType != null && isBypassing(n, hackType));
        }
    }

    private static boolean isBypassing(Player p, Enums.HackType hackType) {
        String key = Enums.Permission.BYPASS.getKey() + (hackType != null ? "." + hackType.toString().toLowerCase() : "");
        return p.hasPermission(key);
    }

    // Separator

    public static boolean isStaff(SpartanPlayer player) {
        if (player.getInstance().isOp()) {
            return true;
        } else {
            for (Enums.Permission permission : staffPermissions) {
                Player realPlayer = player.getInstance();

                if (realPlayer != null
                        && realPlayer.hasPermission(permission.getKey())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static List<SpartanPlayer> getStaff() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();
        int size = players.size();

        if (size > 0) {
            List<SpartanPlayer> array = new ArrayList<>(size);

            for (SpartanPlayer player : players) {
                if (isStaff(player)) {
                    array.add(player);
                }
            }
            return array;
        }
        return new ArrayList<>(0);
    }
}
