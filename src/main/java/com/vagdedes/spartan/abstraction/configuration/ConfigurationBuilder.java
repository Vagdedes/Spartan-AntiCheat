package com.vagdedes.spartan.abstraction.configuration;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConfigurationBuilder {

    public static String getDirectory(String fileName) {
        return Register.plugin.getDataFolder() + "/" + fileName + ".yml";
    }

    protected static final String prefix = "{prefix}";

    // Separator

    protected final File file;
    private final Map<String, Boolean>
            bool = new LinkedHashMap<>(),
            exists = new LinkedHashMap<>();
    private final Map<String, Integer> ints = new LinkedHashMap<>();
    private final Map<String, Double> dbls = new LinkedHashMap<>();
    private final Map<String, String> str = new LinkedHashMap<>();

    public ConfigurationBuilder(String fileName) {
        this.file = new File(getDirectory(fileName));
    }

    protected final YamlConfiguration getPath() {
        if (!file.exists()) {
            create();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    protected final void internalClear() {
        bool.clear();
        exists.clear();
        ints.clear();
        str.clear();
    }

    public final File getFile() {
        return file;
    }

    public final boolean exists(String path) {
        synchronized (this.exists) {
            Boolean data = exists.get(path);

            if (data != null) {
                return data;
            }
            boolean result = getPath().contains(path);
            exists.put(path, result);
            return result;
        }
    }

    public final boolean getBoolean(String path) {
        synchronized (this.bool) {
            Boolean data = bool.get(path);

            if (data != null) {
                return data;
            }
            boolean value = getPath().getBoolean(path);
            bool.put(path, value);
            return value;
        }
    }

    public final int getInteger(String path) {
        synchronized (this.ints) {
            Integer data = ints.get(path);

            if (data != null) {
                return data;
            }
            int value = getPath().getInt(path);
            ints.put(path, value);
            return value;
        }
    }

    public final double getDouble(String path) {
        synchronized (this.dbls) {
            Double data = dbls.get(path);

            if (data != null) {
                return data;
            }
            double value = getPath().getDouble(path);
            dbls.put(path, value);
            return value;
        }
    }

    public final String getString(String path) {
        synchronized (this.str) {
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
    }

    public final String getColorfulString(String path) {
        synchronized (this.str) {
            String data = str.get(path);

            if (data != null) {
                return data;
            }
            if (!file.exists()) {
                create();
            }
            String value = getPath().getString(path);

            if (value == null) {
                return path;
            } else {
                value = ChatColor.translateAlternateColorCodes('&', value);
                value = value.replace(prefix, SpartanEdition.getProductName(value));
            }
            str.put(path, value);
            return value;
        }
    }

    public final void clearOption(String name) {
        synchronized (this.bool) {
            bool.remove(name);
        }
        synchronized (this.exists) {
            exists.remove(name);
        }
        synchronized (this.ints) {
            ints.remove(name);
        }
        synchronized (this.dbls) {
            dbls.remove(name);
        }
        synchronized (this.str) {
            str.remove(name);
        }
    }

    public final void setOption(String name, Object value) {
        ConfigUtils.set(file, name, value);
        clearOption(name);
    }

    public final void addOption(String name, Object value) {
        ConfigUtils.add(file, name, value);
        clearOption(name);
    }

    public final String getOldOption(String old, String current) {
        return exists(old) ? old : current;
    }

    public void clear() {
        internalClear();
    }

    abstract public void create();

    protected void addOption(String option, String value) {
        ConfigUtils.add(file, option, value);
    }
}
