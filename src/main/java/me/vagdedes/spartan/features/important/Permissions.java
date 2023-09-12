package me.vagdedes.spartan.features.important;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Permissions {

    private static final String alternativeAdminKey = "spartan.*";
    private static final Map<UUID, Map<Enums.Permission, Boolean>> perm = new LinkedHashMap<>(Config.getMaxPlayers());
    private static final Map<UUID, Map<Enums.HackType, Boolean>> bypass = new LinkedHashMap<>(Config.getMaxPlayers());
    private static boolean API = false;

    private static final Enums.Permission[] staffPermissions = new Enums.Permission[]{Enums.Permission.STAFF_CHAT, Enums.Permission.WAVE,
            Enums.Permission.WARN, Enums.Permission.ADMIN, Enums.Permission.KICK, Enums.Permission.NOTIFICATIONS,
            Enums.Permission.USE_BYPASS, Enums.Permission.MANAGE, Enums.Permission.INFO, Enums.Permission.UNBAN, Enums.Permission.BAN};

    public static boolean isCacheEnabled() {
        return Settings.getBoolean("Important.use_permission_cache");
    }

    public static void clear() {
        perm.clear();
        bypass.clear();
        API = false;
    }

    // Separator

    public static void add(Player p, Enums.Permission permission) {
        API = true;
        UUID uuid = p.getUniqueId();

        if (permission == Enums.Permission.BYPASS) {
            Map<Enums.HackType, Boolean> map = bypass.get(uuid);

            if (map == null) {
                map = new LinkedHashMap<>(Enums.HackType.values().length);
                map.put(null, true);
                bypass.put(uuid, map);
            } else {
                map.put(null, true);
            }
        } else {
            Map<Enums.Permission, Boolean> map = perm.get(uuid);

            if (map == null) {
                map = new LinkedHashMap<>(Enums.Permission.values().length);
                map.put(permission, true);
                perm.put(uuid, map);
            } else {
                map.put(permission, true);
            }
        }
    }

    // Separator

    public static boolean has(SpartanPlayer p) {
        Player n = p.getPlayer();
        return n != null && n.isOnline() && has(n);
    }

    public static boolean has(Player p) {
        for (Enums.Permission permission : Enums.Permission.values()) {
            if (has(p, permission)) {
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean has(SpartanPlayer p, Enums.Permission permission) {
        Player n = p.getPlayer();
        return n != null && n.isOnline() && has(n, permission);
    }

    public static boolean has(Player p, Enums.Permission permission) {
        if (permission == Enums.Permission.BYPASS) {
            return isBypassing(p, null);
        }
        if (Settings.exists("Important.enable_permissions") && !Settings.getBoolean("Important.enable_permissions")) {
            return true;
        }
        Enums.Permission admin = Enums.Permission.ADMIN;

        if (TestServer.isIdentified() || !isCacheEnabled()) {
            if (p.hasPermission(permission.getKey())) {
                setStaff(p, permission);
                return true;
            }
            if (API) {
                UUID uuid = p.getUniqueId();
                Map<Enums.Permission, Boolean> map = perm.get(uuid);

                if (map != null) {
                    Boolean bool = map.get(permission);

                    if (bool != null && bool) {
                        setStaff(uuid, permission);
                        return true;
                    }
                }
            }
            if (permission != admin ? p.hasPermission(Enums.Permission.ADMIN.getKey()) || p.hasPermission(alternativeAdminKey) :
                    p.hasPermission(alternativeAdminKey)) { // If it's the admin permission and it's false (at this stage it is), then we check the other admin permission
                setStaff(p, permission);
                return true;
            }
            return false;
        }
        UUID uuid = p.getUniqueId();
        Map<Enums.Permission, Boolean> map = perm.get(uuid);

        if (map != null) {
            Boolean bool = map.get(permission);

            if (bool != null) {
                if (bool) {
                    setStaff(uuid, permission);
                }
                return bool;
            }
        } else {
            map = new LinkedHashMap<>(Enums.Permission.values().length);
            perm.put(uuid, map);
        }
        boolean hasPermission = p.hasPermission(permission.getKey());
        boolean adminPermission = permission == admin;

        if (adminPermission) {
            if (!hasPermission) {
                hasPermission = p.hasPermission(alternativeAdminKey);
            }
        } else if (!hasPermission) {
            Boolean bool = map.get(admin);

            if (bool == null) {
                hasPermission = p.hasPermission(Enums.Permission.ADMIN.getKey()) || p.hasPermission(alternativeAdminKey);
                map.put(admin, hasPermission);
            } else {
                hasPermission = bool;
            }
        }
        map.put(permission, hasPermission);

        if (hasPermission) {
            setStaff(uuid, permission);
        }
        return hasPermission;
    }

    // Separator

    public static boolean isBypassing(SpartanPlayer p, Enums.HackType hackType) {
        if (p.isOp()) {
            return Settings.getBoolean("Important.op_bypass");
        }
        Player n = p.getPlayer();
        return n != null && n.isOnline()
                && (isBypassing(n, null)
                || hackType != null && isBypassing(n, hackType));
    }

    public static boolean isBypassing(Player p, Enums.HackType hackType) {
        if (Settings.exists("Important.enable_permissions") && !Settings.getBoolean("Important.enable_permissions")) {
            return true;
        }
        String key = Enums.Permission.BYPASS.getKey() + (hackType != null ? "." + hackType.toString().toLowerCase() : "");

        if (TestServer.isIdentified() || !isCacheEnabled()) {
            if (p.hasPermission(key)) {
                return true;
            }
            if (API) {
                Map<Enums.HackType, Boolean> map = bypass.get(p.getUniqueId());

                if (map != null && map.containsKey(hackType)) { // We use containsKey because the key can be null
                    return map.get(hackType);
                }
            }
            return false;
        }
        UUID uuid = p.getUniqueId();
        Map<Enums.HackType, Boolean> map = bypass.get(uuid);

        if (map != null) {
            if (map.containsKey(hackType)) { // We use containsKey because the key can be null
                return map.get(hackType);
            }
        } else {
            map = new LinkedHashMap<>(Enums.HackType.values().length);
            bypass.put(uuid, map);
        }
        boolean has = p.hasPermission(key);
        map.put(hackType, has);
        return has;
    }

    // Separator

    public static void remove(SpartanPlayer p) {
        remove(p.getUniqueId());
    }

    public static void remove(UUID uuid) {
        perm.remove(uuid);
        bypass.remove(uuid);
    }

    // Separator

    private static void setStaff(UUID uuid, Enums.Permission permission) {
        for (Enums.Permission staffPermission : staffPermissions) {
            if (staffPermission == permission) {
                SpartanPlayer spartanPlayer = SpartanBukkit.getPlayer(uuid);

                if (spartanPlayer != null) {
                    spartanPlayer.getProfile().setStaff(true);
                }
                break;
            }
        }
    }

    private static void setStaff(Player player, Enums.Permission permission) {
        setStaff(player.getUniqueId(), permission);
    }

    // Separator

    public static boolean isStaff(SpartanPlayer player) {
        if (player.isOp()) {
            return true;
        }
        boolean cache = !TestServer.isIdentified() && isCacheEnabled();
        UUID uuid = player.getUniqueId();

        for (Enums.Permission permission : staffPermissions) {
            if (cache) {
                Map<Enums.Permission, Boolean> map = perm.get(uuid);

                if (map != null) {
                    Boolean bool = map.get(permission);

                    if (bool != null && bool) {
                        return true;
                    }
                }
            } else {
                Player realPlayer = player.getPlayer();

                if (realPlayer != null && realPlayer.isOnline() && realPlayer.hasPermission(permission.getKey())) {
                    return true;
                }
            }
        }
        return false;
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
