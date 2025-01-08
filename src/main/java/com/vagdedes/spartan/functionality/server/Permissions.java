package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Permissions {

    private static final String alternativeAdminKey = "spartan.*";
    private static final Enums.Permission[] staffPermissions = new Enums.Permission[]{
            Enums.Permission.WAVE,
            Enums.Permission.WARN,
            Enums.Permission.ADMIN,
            Enums.Permission.KICK,
            Enums.Permission.NOTIFICATIONS,
            Enums.Permission.USE_BYPASS,
            Enums.Permission.MANAGE,
            Enums.Permission.INFO,
    };

    public static boolean has(Player p) {
        for (Enums.Permission permission : Enums.Permission.values()) {
            if (has(p, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean has(Player p, Enums.Permission permission) {
        if (p.hasPermission(permission.getKey())) {
            return true;
        } else {
            return permission != Enums.Permission.ADMIN
                    ? p.hasPermission(Enums.Permission.ADMIN.getKey())
                    || p.hasPermission(alternativeAdminKey)
                    : p.hasPermission(alternativeAdminKey);
        }
    }

    public static boolean onlyHas(Player p, Enums.Permission permission) {
        return p.hasPermission(permission.getKey());
    }

    // Separator

    public static boolean isBypassing(Player p, Enums.HackType hackType) {
        if (p.isOp()) {
            return Config.settings.getBoolean("Important.op_bypass");
        } else {
            String key = Enums.Permission.BYPASS.getKey();

            if (p.hasPermission(key)) {
                return true;
            } else if (hackType != null) {
                return p.hasPermission(key + "." + hackType.toString().toLowerCase());
            } else {
                return false;
            }
        }
    }

    // Separator

    public static boolean isStaff(Player player) {
        if (player.isOp()) {
            return true;
        } else {
            for (Enums.Permission permission : staffPermissions) {
                if (has(player, permission)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static List<SpartanProtocol> getStaff() {
        Collection<SpartanProtocol> protocols = SpartanBukkit.getProtocols();
        int size = protocols.size();

        if (size > 0) {
            List<SpartanProtocol> array = new ArrayList<>(size);

            for (SpartanProtocol protocol : protocols) {
                if (isStaff(protocol.bukkit())) {
                    array.add(protocol);
                }
            }
            return array;
        }
        return new ArrayList<>(0);
    }

}
