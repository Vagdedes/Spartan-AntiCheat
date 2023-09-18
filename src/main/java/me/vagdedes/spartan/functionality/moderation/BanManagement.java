package me.vagdedes.spartan.functionality.moderation;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.interfaces.commands.CommandExecution;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;

public class BanManagement {

    private static File file = new File(Register.plugin.getDataFolder() + "/storage.yml");
    private static final String section = "Bans";
    private static final Map<String, String> saved = new LinkedHashMap<>();
    public static final String
            table = "spartan_bans",
            message = " was banned for ";
    private static final String[] keys = new String[]{
            "reason",
            "punisher",
            "creation",
            "expiration"
    };

    // Base

    public static File getFile() {
        return file;
    }

    public static void clear() {
        saved.clear();
    }

    public static void create(boolean local) {
        file = new File(Register.plugin.getDataFolder() + "/storage.yml");
        boolean exists = file.exists();
        clear();

        // Initiate New Configuration
        if (!exists) {
            set(SpartanBukkit.uuid, keys[0], "Test");
            set(SpartanBukkit.uuid, keys[1], "Unknown");
            set(SpartanBukkit.uuid, keys[2], System.currentTimeMillis());
        }

        // Separator

        if (Config.sql.isEnabled()) {
            SpartanBukkit.storageThread.execute(() -> {
                Config.sql.update("CREATE TABLE IF NOT EXISTS " + BanManagement.table + " (" +
                        "id INT(11) NOT NULL AUTO_INCREMENT, " +

                        "creation BIGINT(20), " +
                        "expiration BIGINT(20), " +
                        "punished VARCHAR(36), " +
                        "punisher VARCHAR(20), " +
                        "reason VARCHAR(" + CommandExecution.maxConnectedArgumentLength + "), " +

                        "primary key (id));");

                try {
                    ResultSet rs = Config.sql.query("SELECT * FROM " + table + " ORDER BY id DESC LIMIT " + ResearchEngine.maxSize + ";");

                    if (rs != null) {
                        while (rs.next()) {
                            String uuid = rs.getString("punished");

                            for (String key : keys) {
                                String value = rs.getString(key);

                                if (value != null) {
                                    saved.put(uuid + "." + key, value);
                                }
                            }
                        }
                        rs.close();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        // Separator

        YamlConfiguration filea = getPath();
        ConfigurationSection configurationSection = filea.getConfigurationSection(section);

        if (configurationSection != null) {
            for (String uuid : configurationSection.getKeys(false)) {
                for (String key : keys) {
                    key = uuid + "." + key;
                    Object value = filea.get(section + "." + key);

                    if (value != null) {
                        saved.put(key, value.toString());
                    }
                }
            }
        }

        // Separator

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    // List

    public static List<UUID> getBanList() {
        int size = saved.size();

        if (size > 0) {
            List<UUID> uuids = new ArrayList<>(size / keys.length);

            for (String key : new HashSet<>(saved.keySet())) {
                try {
                    UUID uuid = UUID.fromString(key.split("\\.", 2)[0]);

                    if (!uuids.contains(uuid) && isBanned(uuid)) {
                        uuids.add(uuid);
                    }
                } catch (Exception ignored) {
                }
            }
            return uuids;
        }
        return new ArrayList<>(0);
    }

    public static String getBanListString() {
        List<UUID> uuids = getBanList();

        if (!uuids.isEmpty()) {
            String comma = ChatColor.GRAY + ", ";
            StringBuilder list = new StringBuilder();

            for (UUID uuid : uuids) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();

                if (name != null) {
                    list.append(ChatColor.RED).append(name).append(comma);
                } else {
                    list.append(ChatColor.RED).append(uuid).append(comma);
                }
            }
            return list.substring(0, list.length() - comma.length());
        }
        return Config.messages.getColorfulString("empty_ban_list");
    }

    // Runnables

    public static void ban(UUID uuid, CommandSender punisher, String reason, long time) {
        long creation = System.currentTimeMillis();
        String punisherName = punisher instanceof ConsoleCommandSender ? Config.messages.getColorfulString("console_name") : punisher.getName(),
                expiration,
                creationDate = new Timestamp(creation).toString().substring(0, 10);
        boolean hasExpiration = time != 0L;

        if (Config.sql.isEnabled()) {
            SpartanBukkit.storageThread.execute(() -> {
                Config.sql.update("DELETE FROM " + table + " WHERE punished = '" + uuid + "';");
                Config.sql.update("INSERT INTO " + table
                        + " (punished, punisher, reason, creation, expiration)" +
                        " VALUES ('" + uuid + "', '" + punisherName + "', '" + reason + "', '" + time + "', " + (hasExpiration ? "'" + time + "'" : "NULL") + ");");
            });
        }
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

        // Separator

        set(uuid, keys[0], reason);
        set(uuid, keys[1], punisherName);
        set(uuid, keys[2], creation);

        if (hasExpiration) {
            set(uuid, keys[3], time);
            expiration = new Timestamp(time).toString().substring(0, 10);
        } else {
            expiration = "Never";
        }

        // Separator

        String message = Config.messages.getColorfulString("ban_broadcast_message")
                .replace("{reason}", reason)
                .replace("{punisher}", punisherName)
                .replace("{creation}", creationDate)
                .replace("{expiration}", expiration);
        message = ConfigUtils.replaceWithSyntax(p, message, null);

        if (Config.settings.getBoolean("Punishments.broadcast_on_punishment")) {
            Bukkit.broadcastMessage(message);
        } else {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (players.size() > 0) {
                for (SpartanPlayer o : players) {
                    if (DetectionNotifications.hasPermission(o)) {
                        o.sendMessage(message);
                    }
                }
            }
        }

        if (p.isOnline()) {
            SpartanPlayer sp = SpartanBukkit.getPlayer((Player) p);

            if (sp != null) {
                SpartanLocation location = sp.getLocation();
                CrossServerInformation.queueNotificationWithWebhook(uuid, p.getName(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                        "Ban", reason,
                        true);
            }
        } else {
            CrossServerInformation.queueNotificationWithWebhook(uuid, p.getName(),
                    0, 0, 0,
                    "Ban", reason,
                    true);
        }

        // Separator

        String kick = Config.messages.getColorfulString("ban_reason")
                .replace("{reason}", reason)
                .replace("{punisher}", punisherName)
                .replace("{creation}", creationDate)
                .replace("{expiration}", expiration);
        kick = ConfigUtils.replaceWithSyntax(p, kick, null);

        if (p.isOnline()) {
            ((Player) p).kickPlayer(kick);
        }

        // Separator

        String name = p.getName();
        AntiCheatLogs.logInfo(Config.getConstruct() + name + BanManagement.message + reason);

        ResearchEngine.getPlayerProfile(name).getPunishmentHistory().increaseBans();
    }

    public static void unban(UUID uuid, boolean force) {
        if (Config.sql.isEnabled()) {
            SpartanBukkit.storageThread.execute(() -> Config.sql.update("DELETE FROM " + table + " WHERE punished = '" + uuid + "';"));
        }
        if (force || isBanned(uuid)) {
            for (String key : keys) {
                key = uuid + "." + key;
                ConfigUtils.set(file, section + "." + key, null);
                saved.remove(key);
            }
            ConfigUtils.set(file, section + "." + uuid, null);
        }
    }

    // Logic

    public static boolean isBanned(UUID uuid) {
        String expiration = get(uuid, keys[3]);
        return (expiration == null || System.currentTimeMillis() <= Long.parseLong(expiration))
                && get(uuid, keys[0]) != null;
    }


    // Dates

    public static long getCreation(UUID uuid) {
        String creation = get(uuid, keys[2]);
        return creation != null ? Long.parseLong(creation) : 0L;
    }

    public static long getExpiration(UUID uuid) {
        String expiration = get(uuid, keys[3]);
        return expiration != null ? Long.parseLong(expiration) : 0L;
    }

    // Utilities

    private static YamlConfiguration getPath() {
        if (!file.exists()) {
            create(false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public static String get(UUID uuid, String path) {
        path = uuid + "." + path;

        if (saved.containsKey(path)) { // Contains key because the value can be null as seen below
            return saved.get(path);
        }
        Object object = getPath().get(section + "." + path);

        if (object != null) {
            String string = object.toString();
            saved.put(path, string);
            return string;
        }
        saved.put(path, null);
        return null;
    }

    public static String getProtected(UUID uuid, String path) {
        String get = get(uuid, path);
        return get == null ? "" : get;
    }

    private static void set(UUID uuid, String path, Object value) {
        path = uuid + "." + path;
        ConfigUtils.set(file, section + "." + path, value);
        saved.put(path, value.toString());
    }

    // Runnable

    public static void run(Player p, PlayerLoginEvent e) {
        UUID uuid = p.getUniqueId();

        if (isBanned(uuid)) {
            String creation = get(uuid, keys[2]),
                    expiration = get(uuid, keys[3]),
                    kick = Config.messages.getColorfulString("ban_reason")
                            .replace("{reason}", getProtected(uuid, keys[0]))
                            .replace("{punisher}", getProtected(uuid, keys[1]))
                            .replace("{expiration}", expiration != null ? new Timestamp(Long.parseLong(expiration)).toString().substring(0, 10) : "Never")
                            .replace("{creation}", creation != null ? new Timestamp(Long.parseLong(creation)).toString().substring(0, 10) : "Unknown");
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, ConfigUtils.replaceWithSyntax(p, kick, null));
        }
    }

}
