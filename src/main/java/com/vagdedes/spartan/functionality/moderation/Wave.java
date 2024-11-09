package com.vagdedes.spartan.functionality.moderation;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Wave {

    private static File file = new File(Register.plugin.getDataFolder() + "/storage.yml");
    private static final String section = "Wave";
    private static final Map<UUID, String> commands
            = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void clearCache() {
        synchronized (commands) {
            commands.clear();
        }
    }

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            synchronized (commands) {
                int size = commands.size();

                if (size > 0) {
                    ConsoleCommandSender sender = Bukkit.getConsoleSender();
                    Iterator<Map.Entry<UUID, String>> iterator = commands.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Map.Entry<UUID, String> entry = iterator.next();
                        iterator.remove();
                        remove(entry.getKey());
                        Bukkit.dispatchCommand(sender, entry.getValue());
                    }
                    end(Config.settings.getBoolean("Punishments.broadcast_on_punishment"), size);
                }
            }
        }, 1L, 1L);
    }

    public static void create() {
        file = new File(Register.plugin.getDataFolder() + "/storage.yml");

        if (!file.exists()) {
            ConfigUtils.add(file, section + "." + UUID.randomUUID() + ".command", "ban {player} wave punishment example");
        }
    }

    // Separator

    public static UUID[] getWaveList() {
        ConfigurationSection configurationSection = YamlConfiguration.loadConfiguration(file).getConfigurationSection(section);

        if (configurationSection != null) {
            List<UUID> list = new LinkedList<>();

            for (String key : configurationSection.getKeys(false)) {
                try {
                    list.add(UUID.fromString(key));
                } catch (Exception ignored) {
                }
            }
            return list.toArray(new UUID[0]);
        }
        return new UUID[]{};
    }

    public static String getWaveListString() {
        StringBuilder list = new StringBuilder();

        for (UUID uuid : getWaveList()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

            if (p.hasPlayedBefore()) {
                list.append(ChatColor.RED).append(p.getName()).append(ChatColor.GRAY).append(", ");
            }
        }
        if (list.length() >= 2) {
            list = new StringBuilder(list.substring(0, list.length() - 2));
        } else if (list.length() == 0) {
            list = new StringBuilder(Config.messages.getColorfulString("empty_wave_list"));
        }
        return list.toString();
    }

    public static String getCommand(UUID uuid) {
        return YamlConfiguration.loadConfiguration(file).getString(section + "." + uuid + ".command");
    }

    // Separator

    public static void add(UUID uuid, String command) {
        ConfigUtils.set(file, section + "." + uuid + ".command", command);

        if (getWaveList().length >= Math.max(Math.min(Bukkit.getMaxPlayers(), 500), 20)) {
            start();
        }
    }

    public static void remove(UUID uuid) {
        String id = section + "." + uuid;
        ConfigUtils.set(file, id + ".command", null);
        ConfigUtils.set(file, id, null);
    }

    public static void clear() {
        for (UUID uuid : getWaveList()) {
            remove(uuid);
        }
    }

    // Separator

    public static boolean start() {
        UUID[] uuids = getWaveList();

        if (uuids.length > 0) {
            synchronized (commands) {
                boolean broadcast = Config.settings.getBoolean("Punishments.broadcast_on_punishment");

                if (broadcast) {
                    Bukkit.broadcastMessage(Config.messages.getColorfulString("wave_start_message"));
                } else {
                    List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

                    if (!protocols.isEmpty()) {
                        String message = Config.messages.getColorfulString("wave_start_message");

                        for (SpartanProtocol protocol : protocols) {
                            if (DetectionNotifications.hasPermission(protocol)) {
                                protocol.bukkit.sendMessage(message);
                            }
                        }
                    }
                }

                SpartanBukkit.dataThread.executeIfFreeElseHere(() -> {
                    for (UUID uuid : uuids) {
                        try {
                            String command = getCommand(uuid);

                            if (command != null) {
                                commands.putIfAbsent(uuid, ConfigUtils.replaceWithSyntax(Bukkit.getOfflinePlayer(uuid), command, null));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
            return true;
        } else {
            return false;
        }
    }

    private static void end(boolean broadcast, int total) {
        if (broadcast) {
            Bukkit.broadcastMessage(
                    Config.messages.getColorfulString("wave_end_message").replace("{total}", String.valueOf(total))
            );
        } else {
            List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

            if (!protocols.isEmpty()) {
                String message = Config.messages.getColorfulString("wave_end_message").replace("{total}", String.valueOf(total));

                for (SpartanProtocol protocol : protocols) {
                    if (DetectionNotifications.hasPermission(protocol)) {
                        protocol.bukkit.sendMessage(message);
                    }
                }
            }
        }
    }

}
