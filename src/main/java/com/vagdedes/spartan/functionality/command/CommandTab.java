package com.vagdedes.spartan.functionality.command;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CommandTab implements TabCompleter {

    private static final Map<String, Enums.Permission[]> commands = new LinkedHashMap<>(18); // Attention

    static {
        commands.put("menu", new Enums.Permission[]{Enums.Permission.INFO, Enums.Permission.MANAGE});
        commands.put("toggle", new Enums.Permission[]{Enums.Permission.MANAGE});
        commands.put("rl", new Enums.Permission[]{Enums.Permission.RELOAD});
        commands.put("reload", new Enums.Permission[]{Enums.Permission.RELOAD});
        commands.put("notifications", new Enums.Permission[]{Enums.Permission.NOTIFICATIONS});
        commands.put("info", new Enums.Permission[]{Enums.Permission.INFO});
        commands.put("kick", new Enums.Permission[]{Enums.Permission.KICK});
        commands.put("warn", new Enums.Permission[]{Enums.Permission.WARN});
        commands.put("bypass", new Enums.Permission[]{Enums.Permission.USE_BYPASS});
        commands.put("wave add", new Enums.Permission[]{Enums.Permission.WAVE});
        commands.put("wave remove", new Enums.Permission[]{Enums.Permission.WAVE});
        commands.put("wave clear", new Enums.Permission[]{Enums.Permission.WAVE});
        commands.put("wave run", new Enums.Permission[]{Enums.Permission.WAVE});
        commands.put("wave list", new Enums.Permission[]{Enums.Permission.WAVE});
        commands.put("customer-support", new Enums.Permission[]{Enums.Permission.ADMIN});
        commands.put("ai-assistance", new Enums.Permission[]{Enums.Permission.ADMIN});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String arg, String[] args) {
        List<String> list = new LinkedList<>();
        int length = args.length;

        if (length == 1) {
            boolean isPlayer = sender instanceof Player;
            Player p = isPlayer ? (Player) sender : null;
            String argAbstract = args[0].toLowerCase();

            for (Map.Entry<String, Enums.Permission[]> entry : commands.entrySet()) {
                boolean add;

                if (!isPlayer) {
                    add = true;
                } else {
                    add = false;

                    for (Enums.Permission permission : entry.getValue()) {
                        if (Permissions.has(p, permission)) {
                            add = true;
                            break;
                        }
                    }
                }

                if (add) {
                    String key = entry.getKey();

                    if (key.contains(argAbstract)) {
                        list.add(key);
                    }
                }
            }
        } else if (length > 1) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                String argAbstract = args[length - 1].toLowerCase();
                boolean player = sender instanceof Player;
                SpartanPlayer p = player ? SpartanBukkit.getPlayer((Player) sender) : null;
                player &= p != null;

                for (SpartanPlayer o : players) {
                    Player on = o.getInstance();

                    if (on != null && (!player || p.canSee(on))) {
                        String name = o.name;

                        if (name.toLowerCase().contains(argAbstract)) {
                            list.add(name);
                        }
                    }
                }
            }
        }
        return list;
    }
}
