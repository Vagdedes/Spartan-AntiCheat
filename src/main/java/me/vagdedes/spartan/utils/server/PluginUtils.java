package me.vagdedes.spartan.utils.server;

import me.vagdedes.spartan.Register;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.List;

public class PluginUtils {

    private static final List<String>
            exists = new ArrayList<>(),
            contains = new ArrayList<>();

    public static void clear() {
        exists.clear();
        contains.clear();
    }

    public static boolean isCommandRegistered(String command) {
        return Bukkit.getPluginCommand(command) != null;
    }

    public static List<Plugin> getDependentPlugins(String independent) {
        Plugin[] plugins = Register.manager.getPlugins();
        List<Plugin> results = new ArrayList<>(plugins.length);

        for (Plugin p : plugins) {
            if (!p.getName().equals(independent)) {
                PluginDescriptionFile descriptionFile = p.getDescription();
                List<String> dependentList = new ArrayList<>(descriptionFile.getDepend());
                dependentList.addAll(descriptionFile.getSoftDepend());

                if (dependentList.size() > 0) {
                    for (String dependent : dependentList) {
                        if (independent.equals(dependent)) {
                            results.add(p);
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    public static boolean contains(String key) {
        if (contains.contains(key)) {
            return true;
        }
        String[] split = key.toLowerCase().split(" ");

        for (Plugin p : Register.manager.getPlugins()) {
            if (p.isEnabled()) {
                String plugin = p.getName().toLowerCase();

                for (String name : split) {
                    if (plugin.contains(name)) {
                        contains.add(key);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean exists(String[] names) {
        for (String name : names) {
            if (exists(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean exists(String name) {
        if (exists.contains(name)) {
            return true;
        }
        for (Plugin p : Register.manager.getPlugins()) {
            if (p.isEnabled() && p.getName().equalsIgnoreCase(name)) {
                exists.add(name);
                return true;
            }
        }
        return false;
    }

    public static Plugin get(String name) {
        for (Plugin p : Register.manager.getPlugins()) {
            if (p.isEnabled() && p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
