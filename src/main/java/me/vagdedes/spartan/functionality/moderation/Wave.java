package me.vagdedes.spartan.functionality.moderation;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.CommonsStringUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Wave {

    private static File file = new File(Register.plugin.getDataFolder() + "/storage.yml");
    private static final String section = "Wave";
    private static final ConcurrentHashMap<UUID, String> commands = new ConcurrentHashMap<>(Config.getMaxPlayers());
    private static boolean run = false, pause = false;

    public static void clearCache() {
        run = false;
        pause = false;
        commands.clear();
    }

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (run) {
                    run = false;
                    int size = commands.size();

                    if (size > 0) {
                        ConsoleCommandSender sender = Bukkit.getConsoleSender();
                        Iterator<Map.Entry<UUID, String>> iterator = commands.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<UUID, String> entry = iterator.next();
                            iterator.remove();
                            Bukkit.dispatchCommand(sender, entry.getValue());
                            remove(entry.getKey());
                        }
                        end(Config.settings.getBoolean("Punishments.broadcast_on_punishment"), size);
                    }
                    pause = false;
                }
            }, 1L, 1L);
        }
    }

    public static void create(boolean local) {
        file = new File(Register.plugin.getDataFolder() + "/storage.yml");
        boolean exists = file.exists();

        if (!exists) {
            ConfigUtils.add(file, section + "." + SpartanBukkit.uuid + ".command", "ban {player} wave punishment example");
        }

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    // Separator

    public static UUID[] getWaveList() {
        ConfigurationSection configurationSection = YamlConfiguration.loadConfiguration(file).getConfigurationSection(section);

        if (configurationSection != null) {
            List<UUID> list = new LinkedList<>();

            for (String key : configurationSection.getKeys(false)) {
                if (key.length() == 36 && CommonsStringUtils.countMatches(key, "-") == 4) {
                    list.add(UUID.fromString(key));
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

    // Separator

    public static void add(UUID uuid, String command) {
        ConfigUtils.set(file, section + "." + uuid + ".command", command);

        if (getWaveList().length >= Config.getMaxPlayers()) {
            start();
        }
    }

    // Separator

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

    public static String getCommand(UUID uuid) {
        YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);
        return filea.getString(section + "." + uuid + ".command");
    }

    public static boolean start() {
        if (!pause) {
            pause = true;
            boolean broadcast = Config.settings.getBoolean("Punishments.broadcast_on_punishment");

            if (broadcast) {
                Bukkit.broadcastMessage(Config.messages.getColorfulString("wave_start_message"));
            } else {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    String message = Config.messages.getColorfulString("wave_start_message");

                    for (SpartanPlayer o : players) {
                        if (DetectionNotifications.hasPermission(o)) {
                            o.sendMessage(message);
                        }
                    }
                }
            }

            SpartanBukkit.storageThread.executeIfFreeElseHere(() -> {
                UUID[] uuids = getWaveList();

                if (uuids.length > 0) {
                    for (UUID uuid : uuids) {
                        try {
                            String command = getCommand(uuid);

                            if (command != null) {
                                commands.putIfAbsent(uuid, ConfigUtils.replaceWithSyntax(Bukkit.getOfflinePlayer(uuid), command, null));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    run = true;
                } else {
                    end(broadcast, 0);
                    pause = false;
                }
            });
            return true;
        }
        return false;
    }

    private static void end(boolean broadcast, int total) {
        if (broadcast) {
            Bukkit.broadcastMessage(
                    Config.messages.getColorfulString("wave_end_message").replace("{total}", String.valueOf(total))
            );
        } else {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                String message = Config.messages.getColorfulString("wave_end_message").replace("{total}", String.valueOf(total));

                for (SpartanPlayer o : players) {
                    if (DetectionNotifications.hasPermission(o)) {
                        o.sendMessage(message);
                    }
                }
            }
        }
    }
}
