package me.vagdedes.spartan.abstraction;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConfigurationBuilder {

    public static String getDirectory(String fileName) {
        return Register.plugin.getDataFolder() + "/" + fileName + ".yml";
    }

    protected File file;
    protected final String directory;

    protected static final String prefix = "{prefix}";

    private final Map<String, Boolean>
            bool = new LinkedHashMap<>(),
            exists = new LinkedHashMap<>();
    private final Map<String, Integer> ints = new LinkedHashMap<>();
    private final Map<String, String> str = new LinkedHashMap<>();

    public ConfigurationBuilder(String fileName) {
        this.directory = getDirectory(fileName);
        this.file = new File(directory);
    }

    protected YamlConfiguration getPath() {
        if (!file.exists()) {
            create(false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    protected void internalClear() {
        bool.clear();
        exists.clear();
        ints.clear();
        str.clear();
    }

    public File getFile() {
        return file;
    }

    public boolean exists(String path) {
        Boolean data = exists.get(path);

        if (data != null) {
            return data;
        }
        boolean result = getPath().contains(path);
        exists.put(path, result);
        return result;
    }

    public boolean getBoolean(String path) {
        Boolean data = bool.get(path);

        if (data != null) {
            return data;
        }
        boolean value = getPath().getBoolean(path);
        bool.put(path, value);
        return value;
    }

    public int getInteger(String path) {
        Integer data = ints.get(path);

        if (data != null) {
            return data;
        }
        int value = getPath().getInt(path);
        ints.put(path, value);
        return value;
    }

    public String getString(String path) {
        String data = str.get(path);

        if (data != null) {
            return data;
        }
        String value = getPath().getString(path);

        if (value == null) {
            return path;
        }
        str.put(path, value);
        return value;
    }

    public String getColorfulString(String path) {
        String data = str.get(path);

        if (data != null) {
            return data;
        }
        if (!file.exists()) {
            create(false);
        }
        String value = getPath().getString(path);

        if (value == null) {
            return path;
        } else {
            value = ChatColor.translateAlternateColorCodes('&', value)
                    .replace(prefix, SpartanEdition.getProductName(false));
        }
        str.put(path, value);
        return value;
    }

    public void clearOption(String name) {
        bool.remove(name);
        exists.remove(name);
        ints.remove(name);
        str.remove(name);
    }

    public void setOption(String name, Object value) {
        ConfigUtils.set(file, name, value);
        clearOption(name);
    }

    public void addOption(String name, Object value) {
        ConfigUtils.add(file, name, value);
        clearOption(name);
    }

    public String getOldOption(String old, String current) {
        return exists(old) ? old : current;
    }

    abstract public void clear();

    abstract public void create(boolean local);

    protected void addOption(String option, String value) {
        ConfigUtils.add(file, option, value);
    }
}
