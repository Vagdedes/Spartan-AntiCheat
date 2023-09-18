package me.vagdedes.spartan.functionality.protections;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReconnectCooldown {

    private static final Map<UUID, Long> cooldown = new LinkedHashMap<>(Config.getMaxPlayers());

    public static void run(Player p, PlayerLoginEvent e) {
        int config_cooldown = Config.settings.getInteger("reconnect_cooldown");

        if (config_cooldown > 0 && !Permissions.has(p, Enums.Permission.RECONNECT)) {
            Long ms = cooldown.get(p.getUniqueId());

            if (ms != null) {
                long time = System.currentTimeMillis() - ms;

                if (time <= config_cooldown * 1000L) {
                    String kick = Config.messages.getColorfulString("reconnect_kick_message").replace("{time}", String.valueOf(config_cooldown - ((int) (time / 1000L))));
                    e.disallow(PlayerLoginEvent.Result.KICK_OTHER, ConfigUtils.replaceWithSyntax(p, kick, null));
                }
            }
        }
    }

    public static void clear() {
        cooldown.clear();
    }

    public static void remove(Player p) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public static void loadCooldowns() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                remove(p.getPlayer());
            }
        }
    }
}
