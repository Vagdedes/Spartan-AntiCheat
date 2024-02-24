package com.vagdedes.spartan.configuration;

import com.vagdedes.spartan.abstraction.ConfigurationBuilder;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SQLFeature extends ConfigurationBuilder {

    public SQLFeature() {
        super("sql");
    }

    private static boolean enabled = false;

    private static Connection con = null;
    private static final List<String> list = Collections.synchronizedList(new ArrayList<>());

    // Separator

    @Override
    public void clear() {
        internalClear();
    }

    public String getHost() {
        String result = getString("host");

        if (result != null) {
            result = result.toLowerCase().replace("localhost", "127.0.0.1").replace(" ", "");
        }
        return result;
    }

    public String getUser() {
        String result = getString("user");

        if (result != null) {
            result = result.replace(" ", "");
        }
        return result;
    }

    public String getPassword() {
        String result = getString("password");

        if (result != null && getBoolean("escape_special_characters")) {
            result = StringUtils.escapeMetaCharacters(result);
        }
        return result;
    }

    public String getDatabase() {
        String result = getString("database");

        if (result != null) {
            result = result.replace(" ", "");
        }
        return result;
    }

    public String getTable() {
        String result = getString("table");

        if (result != null) {
            result = result.replace(" ", "");
        }
        return result;
    }

    public String getPort() {
        String result = getString("port");

        if (result != null) {
            result = result.replace(" ", "");
            Double decimal = AlgebraUtils.returnValidDecimal(result);

            if (decimal != null) {
                result = String.valueOf(AlgebraUtils.integerFloor(decimal));
            }
            return result;
        } else {
            return null;
        }
    }

    public String getDriver() {
        String result = getString("driver");

        if (result == null) {
            result = "mysql";
        }
        return result;
    }

    public String getTLSVersion() {
        return getString("tls_Version");
    }

    public boolean getSSL() {
        return getBoolean("use_SSL");
    }

    public boolean getPublicKeyRetrieval() {
        return getBoolean("allow_public_key_retrieval");
    }

    // Separator

    public void refreshConfiguration() {
        clear();
        // Always after cache is cleared
        enabled = !getHost().isEmpty()
                && !getUser().isEmpty()
                && !getPassword().isEmpty()
                && !getDatabase().isEmpty()
                && !getTable().isEmpty()
                && !getDriver().isEmpty();
    }

    public void refreshDatabase() {
        // Queries
        if (!list.isEmpty()) {
            synchronized (list) {
                for (String insert : list) {
                    update(insert);
                }
                list.clear();
            }
        }

        if (isConnected(false)) {
            try {
                con.close();
            } catch (Exception ignored) {
            }
        }
        con = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Separator

    @Override
    public void create(boolean local) {
        file = new File(directory);
        boolean exists = file.exists();
        addOption("host", "");
        addOption("user", "");
        addOption("password", "");
        addOption("database", "");
        addOption("table", "spartan_logs");
        addOption("port", "3306");
        addOption("driver", "mysql");
        addOption("tls_Version", "");
        addOption("use_SSL", true);
        addOption("allow_public_key_retrieval", false);
        addOption("escape_special_characters", false);
        refreshConfiguration();

        SpartanBukkit.storageThread.execute(this::connect);

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    // Separator

    private boolean isConnected(boolean message) {
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

    public void connect() {
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
            } else if (table.isEmpty()) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Table is blank");
            } else if (!AlgebraUtils.validInteger(port) && !AlgebraUtils.validDecimal(port)) {
                enabled = false;
                AwarenessNotifications.forcefullySend("SQL Configuration Error: Port is not a valid number");
            } else if (!isConnected(true)) {
                String driver = getDriver();

                try {
                    if (driver.isEmpty()) {
                        AwarenessNotifications.forcefullySend("SQL Configuration Error: Driver is blank");
                    } else {
                        String tlsVersion = getTLSVersion();
                        con = DriverManager.getConnection("jdbc:" + driver + "://" + host + ":" + port + "/" + database + "?" +
                                        "autoReconnect=true" +
                                        "&maxReconnects=10" +
                                        (tlsVersion != null && !tlsVersion.isEmpty() ? "&enabledTLSProtocols=TLSv" + tlsVersion : "") +
                                        "&useSSL=" + getSSL() +
                                        "&allowPublicKeyRetrieval=" + getPublicKeyRetrieval(),
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

    public void update(String command) {
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

    public ResultSet query(final String command) {
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

    private void createTable(String table) {
        update(
                "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id INT(11) NOT NULL AUTO_INCREMENT, " +
                        "creation_date VARCHAR(30), " +

                        "plugin_version VARCHAR(16), " +
                        "server_version VARCHAR(7), " +
                        "server_tps DOUBLE, " +
                        "online_players INT(11), " +

                        "type VARCHAR(32), " +
                        "information VARCHAR(4096), " +

                        "player_uuid VARCHAR(36), " +
                        "player_latency INT(11), " +
                        "player_x INT(11), " +
                        "player_y INT(11), " +
                        "player_z INT(11), " +

                        "functionality VARCHAR(32), " +
                        "violation_level INT(3), " +

                        "primary key (id));"
        );
    }

    // Separator

    public void logInfo(SpartanPlayer p,
                        String information,
                        Material material,
                        Enums.HackType hackType,
                        boolean miningNotification,
                        int violations) {
        if (enabled) {
            String table = getTable();
            boolean hasPlayer = p != null;
            boolean hasCheck = hackType != null;
            UUID uuid = hasPlayer ? p.getUniqueId() : null;
            SpartanLocation location = hasPlayer ? p.getLocation() : null;

            synchronized (list) {
                list.add(
                        "INSERT INTO " + table
                                + " (creation_date"
                                + ", plugin_version, server_version, server_tps, online_players"
                                + ", type, information"
                                + ", player_uuid, player_latency, player_x, player_y, player_z"
                                + ", functionality, violation_level) "
                                + "VALUES (" + syntaxForColumn(DateTimeFormatter.ofPattern(AntiCheatLogs.dateFormat).format(LocalDateTime.now()))
                                + ", " + syntaxForColumn(API.getVersion())
                                + ", " + syntaxForColumn(MultiVersion.versionString())
                                + ", " + syntaxForColumn(TPS.get(p, false))
                                + ", " + syntaxForColumn(SpartanBukkit.getPlayerCount())
                                + ", " + syntaxForColumn(miningNotification ? "mining" : hasCheck ? "violation" : "other")
                                + ", " + syntaxForColumn(information)
                                + ", " + (hasPlayer ? syntaxForColumn(uuid) : "NULL")
                                + ", " + (hasPlayer ? syntaxForColumn(p.getPing()) : "NULL")
                                + ", " + (hasPlayer ? syntaxForColumn(location.getBlockX()) : "NULL")
                                + ", " + (hasPlayer ? syntaxForColumn(location.getBlockY()) : "NULL")
                                + ", " + (hasPlayer ? syntaxForColumn(location.getBlockZ()) : "NULL")
                                + ", " + (hasCheck ? syntaxForColumn(hackType) : miningNotification ? syntaxForColumn(material) : "NULL")
                                + ", " + (violations > -1 ? syntaxForColumn(violations) : "NULL")
                                + ");"
                );
            }

            if (list.size() >= 10) {
                Runnable runnable = () -> {
                    List<String> newList;

                    synchronized (list) {
                        newList = new ArrayList<>(list);
                        list.clear();
                    }
                    for (String insert : newList) {
                        update(insert);
                    }
                };

                if (SpartanBukkit.isSynchronised()) {
                    SpartanBukkit.storageThread.execute(runnable);
                } else {
                    runnable.run();
                }
            }
        }
    }

    private String syntaxForColumn(Object obj) {
        return "'" + obj.toString() + "'";
    }
}
