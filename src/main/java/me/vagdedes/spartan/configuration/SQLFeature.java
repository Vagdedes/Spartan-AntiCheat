package me.vagdedes.spartan.configuration;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.features.configuration.AntiCheatLogs;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.moderation.BanManagement;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.features.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SQLFeature {

    private static File file = new File(Register.plugin.getDataFolder() + "/sql.yml");
    private static final Map<String, String> saved = new LinkedHashMap<>(8);
    private static final Map<String, Boolean> bool = new LinkedHashMap<>(1);
    private static boolean enabled = false;

    private static Connection con = null;
    private static final List<String> list = new CopyOnWriteArrayList<>();

    private static YamlConfiguration getPath() {
        if (!file.exists()) {
            create(false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // Separator

    public static String getHost() {
        String data = saved.get("host");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("host");

        if (result != null) {
            result = result.toLowerCase().replace("localhost", "127.0.0.1").replace(" ", "");
        }
        saved.put("host", result);
        return result;
    }

    public static String getUser() {
        String data = saved.get("user");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("user");

        if (result != null) {
            result = result.replace(" ", "");
        }
        saved.put("user", result);
        return result;
    }

    public static String getPassword() {
        String data = saved.get("password");

        if (data != null) {
            return data;
        }
        YamlConfiguration path = getPath();
        String result = path.getString("password");

        if (result != null && path.getBoolean("escape_special_characters")) {
            result = StringUtils.escapeMetaCharacters(result);
        }
        saved.put("password", result);
        return result;
    }

    public static String getDatabase() {
        String data = saved.get("database");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("database");

        if (result != null) {
            result = result.replace(" ", "");
        }
        saved.put("database", result);
        return result;
    }

    public static String getTable() {
        String data = saved.get("table");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("table");

        if (result != null) {
            result = result.replace(" ", "");
        }
        saved.put("table", result);
        return result;
    }

    public static String getPort() {
        String data = saved.get("port");

        if (data != null) {
            return data;
        }
        Object result = getPath().get("port");

        if (result != null) {
            String string = result.toString().replace(" ", "");
            Double decimal = AlgebraUtils.returnValidDecimal(string);

            if (decimal != null) {
                string = String.valueOf(AlgebraUtils.integerFloor(decimal));
            }
            saved.put("port", string);
            return string;
        }
        saved.put("port", null);
        return null;
    }

    public static String getDriver() {
        String data = saved.get("driver");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("driver");

        if (result == null) {
            result = "mysql";
        }
        saved.put("driver", result);
        return result;
    }

    public static String getTLSVersion() {
        String data = saved.get("tls");

        if (data != null) {
            return data;
        }
        String result = getPath().getString("tls_Version");
        saved.put("tls", result);
        return result;
    }

    public static boolean getSSL() {
        Boolean data = bool.get("ssl");

        if (data != null) {
            return data;
        }
        boolean result = getPath().getBoolean("use_SSL");
        bool.put("ssl", result);
        return result;
    }

    // Separator

    public static void refreshConfiguration() {
        saved.clear();
        bool.clear();

        // Always after cache is cleared
        enabled = getHost().length() > 0
                && getUser().length() > 0
                && getPassword().length() > 0
                && getDatabase().length() > 0
                && getTable().length() > 0
                && getDriver().length() > 0;
    }

    public static void refreshDatabase() {
        // Queries
        if (list.size() > 0) {
            for (String insert : list) {
                update(insert);
            }
            list.clear();
        }

        if (isConnected(false)) {
            try {
                con.close();
            } catch (Exception ignored) {
            }
        }
        con = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // Separator

    public static void create(boolean local) {
        file = new File(Register.plugin.getDataFolder() + "/mysql.yml");

        if (!file.exists()) {
            file = new File(Register.plugin.getDataFolder() + "/sql.yml");
        }
        boolean exists = file.exists();
        ConfigUtils.add(file, "host", "");
        ConfigUtils.add(file, "user", "");
        ConfigUtils.add(file, "password", "");
        ConfigUtils.add(file, "database", "");
        ConfigUtils.add(file, "table", "spartan_logs");
        ConfigUtils.add(file, "port", "3306");
        ConfigUtils.add(file, "driver", "mysql");
        ConfigUtils.add(file, "tls_Version", "");
        ConfigUtils.add(file, "use_SSL", true);
        ConfigUtils.add(file, "escape_special_characters", false);
        refreshConfiguration();

        SpartanBukkit.storageThread.execute(SQLFeature::connect);

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    // Separator

    private static boolean isConnected(boolean message) {
        if (con != null) {
            try {
                return !con.isClosed();
            } catch (Exception e) {
                if (message) {
                    AwarenessNotifications.forcefullySend("SQL Connection Check Error:\n" + e.getMessage());
                }
            }
        }
        return false;
    }

    public static void connect() {
        String host = getHost(),
                user = getUser(),
                password = getPassword(),
                database = getDatabase(),
                table = getTable(),
                port = getPort();
        int hostLength = host.length(),
                userLength = user.length(),
                passwordLength = password.length(),
                databaseLength = database.length();

        // Check if the user has configured the config even a little
        if (hostLength > 0 || userLength > 0 || passwordLength > 0 || databaseLength > 0) { // Do not check table, port & driver
            if (hostLength == 0) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Host is blank");
            } else if (userLength == 0) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: User is blank");
            } else if (passwordLength == 0) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Password is blank");
            } else if (databaseLength == 0) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Database is blank");
            } else if (table.length() == 0) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Table is blank");
            } else if (!AlgebraUtils.validInteger(port) && !AlgebraUtils.validDecimal(port)) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Port is not a valid number");
            } else if (table.equalsIgnoreCase(BanManagement.table)) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Table cannot have the name: " + BanManagement.table);
            } else if (!isConnected(true)) {
                String driver = getDriver();

                try {
                    if (driver.length() == 0) {
                        AwarenessNotifications.forcefullySend("SQL Configuration Error: Driver is blank");
                    } else {
                        String tlsVersion = getTLSVersion();
                        con = DriverManager.getConnection("jdbc:" + driver + "://" + host + ":" + port + "/" + database + "?" +
                                        "autoReconnect=true" +
                                        "&maxReconnects=10" +
                                        (tlsVersion != null && tlsVersion.length() > 0 ? "&enabledTLSProtocols=TLSv" + tlsVersion : "") +
                                        "&useSSL=" + getSSL(),
                                user, password);
                        createTable(table);
                    }
                } catch (SQLException e) {
                    AwarenessNotifications.forcefullySend(Config.getConstruct() + "SQL Initial Connection Error:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    // Separator

    public static void update(String command) {
        connect();

        try {
            if (con != null) {
                Statement st = con.createStatement();
                st.executeUpdate(command);
                st.close();
            }
        } catch (Exception e) {
            AwarenessNotifications.forcefullySend("SQL Update Error:\n"
                    + "Command: " + command + "\n"
                    + "Exception: " + e.getMessage());
        }
    }

    public static ResultSet query(final String command) {
        connect();
        ResultSet rs = null;

        try {
            if (con != null) {
                final Statement st = con.createStatement();
                rs = st.executeQuery(command);
            }
        } catch (Exception e) {
            AwarenessNotifications.forcefullySend("SQL Query Error:\n"
                    + "Command: " + command + "\n"
                    + "Exception: " + e.getMessage());
        }
        return rs;
    }

    // Separator

    private static void createTable(String table) {
        update(
                "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id INT(11) NOT NULL AUTO_INCREMENT, " +
                        "creation_date VARCHAR(30), " +

                        "plugin_version VARCHAR(16), " +
                        "server_version VARCHAR(7), " +
                        "server_tps DOUBLE, " +
                        "online_players INT(11), " +

                        "type VARCHAR(32), " +
                        "information VARCHAR(" + ResearchEngine.maxDataLength + "), " +

                        "player_uuid VARCHAR(36), " +
                        "player_latency INT(11), " +
                        "player_x INT(11), " +
                        "player_y INT(11), " +
                        "player_z INT(11), " +

                        "functionality VARCHAR(32), " +
                        "violation_level INT(3), " +
                        "cancel_violation INT(3), " +

                        "primary key (id));"
        );
    }

    // Separator

    public static void logInfo(SpartanPlayer p, String information,
                               Material material, Enums.HackType hackType,
                               boolean falsePositive, boolean miningNotification,
                               int violations, int cancelViolation) {
        if (enabled) {
            String table = getTable();
            boolean hasPlayer = p != null;
            boolean hasCheck = hackType != null;
            UUID uuid = hasPlayer ? p.getUniqueId() : null;
            SpartanLocation location = hasPlayer ? p.getLocation() : null;

            list.add("INSERT INTO " + table
                    + " (creation_date"
                    + ", plugin_version, server_version, server_tps, online_players"
                    + ", type, information"
                    + ", player_uuid, player_latency, player_x, player_y, player_z"
                    + ", functionality, violation_level, cancel_violation) "
                    + "VALUES (" + syntaxForColumn(DateTimeFormatter.ofPattern(AntiCheatLogs.dateFormat).format(LocalDateTime.now()))
                    + ", " + syntaxForColumn(API.getVersion())
                    + ", " + syntaxForColumn(MultiVersion.versionString())
                    + ", " + syntaxForColumn(TPS.get(p, false))
                    + ", " + syntaxForColumn(SpartanBukkit.getPlayerCount())
                    + ", " + syntaxForColumn(hasCheck ? (falsePositive ? "false-positive" : "violation") : miningNotification ? "mining" : "other")
                    + ", " + syntaxForColumn(information)
                    + ", " + (hasPlayer ? syntaxForColumn(uuid) : "NULL")
                    + ", " + (hasPlayer ? syntaxForColumn(p.getPing()) : "NULL")
                    + ", " + (hasPlayer ? syntaxForColumn(location.getBlockX()) : "NULL")
                    + ", " + (hasPlayer ? syntaxForColumn(location.getBlockY()) : "NULL")
                    + ", " + (hasPlayer ? syntaxForColumn(location.getBlockZ()) : "NULL")
                    + ", " + (hasCheck ? syntaxForColumn(hackType) : miningNotification ? syntaxForColumn(material) : "NULL")
                    + ", " + (violations > -1 ? syntaxForColumn(violations) : "NULL")
                    + ", " + (cancelViolation > -1 && hasCheck && !hackType.getCheck().isSilent(hasPlayer ? location.getWorld().getName() : null, uuid) ? syntaxForColumn(cancelViolation) : "NULL")
                    + ");");

            if (list.size() >= Check.sufficientViolations) {
                Runnable runnable = () -> {
                    for (String insert : list) {
                        update(insert);
                    }
                    list.clear();
                };

                if (SpartanBukkit.isSynchronised()) {
                    SpartanBukkit.storageThread.execute(runnable);
                } else {
                    runnable.run();
                }
            }
        }
    }

    private static String syntaxForColumn(Object obj) {
        return "'" + obj.toString() + "'";
    }
}
