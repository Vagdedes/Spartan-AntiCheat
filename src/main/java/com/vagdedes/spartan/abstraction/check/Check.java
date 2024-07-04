package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.api.CheckSilentToggleEvent;
import me.vagdedes.spartan.api.CheckToggleEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Check {

    // Static

    public static final int
            maxCommands = 10,
            standardIgnoredViolations = 3;

    // Object

    public final Enums.HackType hackType;
    private String name;
    private final Map<String, Object> options;
    private boolean silent;
    public final boolean handleCancelledEvents;
    private final Map<Enums.DataType, Map<Integer, Integer>> ignoredViolations;
    private final boolean[] enabled;
    public final boolean
            canPunish,
            supportsSilent;
    private final String[]
            disabledWorlds,
            silentWorlds;

    // Object Methods

    public Check(Enums.HackType hackType) {
        this(hackType, new LinkedHashMap<>(), false);
    }

    public Check(Enums.HackType hackType,
                 Map<Enums.DataType, Map<Integer, Integer>> ignoredViolations,
                 boolean copy) {
        this.options = Collections.synchronizedMap(new LinkedHashMap<>());
        this.hackType = hackType;
        this.ignoredViolations = copy ? ignoredViolations : Collections.synchronizedMap(ignoredViolations);

        // Separator

        Object silent = getOption("silent", false, false),
                handleCancelledEvents = getOption("cancelled_event", false, false);

        String name = getOption("name", this.hackType.toString(), false).toString(),
                worlds_config = getOption("disabled_worlds", "exampleDisabledWorld1, exampleDisabledWorld2", false).toString(),
                silents_config = getOption("silent_worlds", "exampleSilentWorld1, exampleSilentWorld2", false).toString();

        // Separator
        Enums.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);
        this.enabled = new boolean[ResearchEngine.usableDataTypes.length];
        Object oldOptionValue = getOption("enabled", null, false);
        boolean hasOldOption = oldOptionValue instanceof Boolean;

        if (hasOldOption) {
            setOption("enabled", null);
        }

        for (Enums.DataType dataType : dataTypes) {
            Object optionValue = getOption(
                    "enabled." + dataType.toString().toLowerCase(),
                    hasOldOption ? oldOptionValue : true,
                    false
            );
            this.enabled[dataType.ordinal()] = optionValue instanceof Boolean ? (boolean) optionValue :
                    optionValue instanceof Long || optionValue instanceof Integer || optionValue instanceof Short ? ((long) optionValue) > 0L :
                            optionValue instanceof Double || optionValue instanceof Float ? ((double) optionValue) > 0.0 :
                                    Boolean.parseBoolean(optionValue.toString().toLowerCase());
        }

        Object punish = getOption("punish", hackType != Enums.HackType.GhostHand, false); // GhostHand: can punish by default
        this.canPunish = punish instanceof Boolean ? (boolean) punish :
                punish instanceof Long || punish instanceof Integer || punish instanceof Short ? ((long) punish) > 0L :
                        punish instanceof Double || punish instanceof Float ? ((double) punish) > 0.0 :
                                Boolean.parseBoolean(punish.toString().toLowerCase());

        // Separator

        if (name != null) {
            this.name = name;
        } else {
            this.name = hackType.toString();
        }

        // Separator

        if (silent != null) {
            this.supportsSilent = true;

            if (silent instanceof Boolean) {
                this.silent = (boolean) silent;
            } else if (silent instanceof Long || silent instanceof Integer || silent instanceof Short) {
                this.silent = ((long) silent) > 0L;
            } else if (silent instanceof Double || silent instanceof Float) {
                this.silent = ((double) silent) > 0.0;
            } else {
                this.silent = Boolean.parseBoolean(silent.toString().toLowerCase());
            }
        } else {
            this.supportsSilent = false;
            this.silent = false;
        }

        // Separator

        if (handleCancelledEvents != null) {
            if (handleCancelledEvents instanceof Boolean) {
                this.handleCancelledEvents = (boolean) handleCancelledEvents;
            } else if (handleCancelledEvents instanceof Long || handleCancelledEvents instanceof Integer || handleCancelledEvents instanceof Short) {
                this.handleCancelledEvents = ((long) handleCancelledEvents) > 0L;
            } else if (handleCancelledEvents instanceof Double || handleCancelledEvents instanceof Float) {
                this.handleCancelledEvents = ((double) handleCancelledEvents) > 0.0;
            } else {
                this.handleCancelledEvents = Boolean.parseBoolean(handleCancelledEvents.toString().toLowerCase());
            }
        } else {
            this.handleCancelledEvents = false;
        }

        // Separator

        if (worlds_config != null) {
            String[] worldsSplit = worlds_config.split(",");
            int size = worldsSplit.length;

            if (size > 0) {
                Set<String> set = new HashSet<>(size);

                for (String world : worldsSplit) {
                    world = world.replace(" ", "");

                    if (!world.isEmpty()) {
                        set.add(world.toLowerCase());
                    }
                }
                if (!set.isEmpty()) {
                    this.disabledWorlds = set.toArray(new String[0]);
                } else {
                    this.disabledWorlds = new String[]{};
                }
            } else {
                this.disabledWorlds = new String[]{};
            }
        } else {
            this.disabledWorlds = new String[]{};
        }

        // Separator

        if (supportsSilent && silents_config != null) {
            String[] worldsSplit = silents_config.split(",");
            int size = worldsSplit.length;

            if (size > 0) {
                Set<String> set = new HashSet<>(size);

                for (String world : worldsSplit) {
                    world = world.replace(" ", "");

                    if (!world.isEmpty()) {
                        set.add(world.toLowerCase());
                    }
                }
                if (!set.isEmpty()) {
                    this.silentWorlds = set.toArray(new String[0]);
                } else {
                    this.silentWorlds = new String[]{};
                }
            } else {
                this.silentWorlds = new String[]{};
            }
        } else {
            this.silentWorlds = new String[]{};
        }
    }

    public void clearConfigurationCache() {
        synchronized (options) {
            options.clear();
        }
    }

    // Separator

    public boolean isEnabled(Enums.DataType dataType, String world, SpartanPlayer player) {
        Enums.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

        if (dataType == null) {
            for (Enums.DataType type : dataTypes) {
                if (this.enabled[type.ordinal()]) {
                    return true;
                }
            }
            return false;
        } else if (!this.enabled[dataType.ordinal()]) {
            return false;
        }
        return (world == null || isEnabledOnWorld(world))
                && (player == null
                || player.getViolations(hackType).getDisableCause() == null
                && !Permissions.isBypassing(player, hackType));
    }

    public void setEnabled(Enums.DataType dataType, boolean b) {
        Enums.DataType[] dataTypes;

        if (dataType == null) {
            dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);
        } else {
            dataTypes = null;

            for (Enums.DataType type : ResearchEngine.getDynamicUsableDataTypes(false)) {
                if (type == dataType) {
                    dataTypes = new Enums.DataType[]{dataType};
                    break;
                }
            }

            if (dataTypes == null) {
                return;
            }
        }
        for (Enums.DataType type : dataTypes) {
            int ordinal = type.ordinal();

            if (enabled[ordinal] != b) {
                CheckToggleEvent event;

                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new CheckToggleEvent(this.hackType, b ? Enums.ToggleAction.ENABLE : Enums.ToggleAction.DISABLE);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }

                if (event == null || !event.isCancelled()) {
                    this.enabled[ordinal] = b;

                    if (!b) {
                        clearConfigurationCache();

                        for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
                            player.getViolations(hackType).reset();
                        }
                    }
                    setOption("enabled." + type.toString().toLowerCase(), b);
                }
            }
        }
    }

    public boolean isEnabledOnWorld(String world) {
        if (disabledWorlds.length > 0) {
            world = world.toLowerCase();

            for (String disabledWorld : disabledWorlds) {
                if (disabledWorld.equals(world)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String[] getDisabledWorlds() {
        return disabledWorlds;
    }

    // Separator

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (!hackType.equals(this.hackType) && name.equalsIgnoreCase(hackType.toString())) {
                return;
            }
        }
        this.name = name;
        setOption("name", name);
    }

    // Separator

    private boolean setOption(String option, Object value) {
        File file = Config.getFile();

        try {
            if (file.exists() || file.createNewFile()) {
                String key = this.hackType + "." + option;
                YamlConfiguration configuration = Config.getConfiguration();

                if (configuration != null) {
                    configuration.set(key, value);

                    try {
                        configuration.save(file);
                        options.remove(key); // Remove instead of modifying to be on demand and have the chance to catch changes by the user
                    } catch (Exception ex) {
                        AwarenessNotifications.forcefullySend("Failed to store '" + key + "' option in '" + file.getName() + "' file.");
                        ex.printStackTrace();
                    }
                } else {
                    AwarenessNotifications.forcefullySend("Failed to load checks configuration (1).");
                }
                return true;
            }
        } catch (Exception ex) {
            AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
            ex.printStackTrace();
        }
        AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
        return false;
    }

    public Collection<String> getOptionKeys() {
        synchronized (options) {
            return new ArrayList<>(options.keySet());
        }
    }

    public Collection<Object> getOptionValues() {
        synchronized (options) {
            return new ArrayList<>(options.values());
        }
    }

    public Collection<Map.Entry<String, Object>> getOptions() {
        synchronized (options) {
            return new ArrayList<>(options.entrySet());
        }
    }

    public Set<String[]> getStoredOptions() {
        File file = Config.getFile();

        if (file.exists()) {
            Set<String[]> set = new LinkedHashSet<>(30);
            YamlConfiguration configuration = Config.getConfiguration();

            if (configuration != null) {
                String hackTypeString = this.hackType.toString();

                for (String key : configuration.getKeys(true)) {
                    if (key.split("\\.", 2)[0].equalsIgnoreCase(hackTypeString)) {
                        Object option = configuration.get(key, null);

                        if (option != null) {
                            set.add(new String[]{key, option.toString()});
                        }
                    }
                }
            }
            return set;
        }
        return new HashSet<>(0);
    }

    public Object getOption(String option, Object def, boolean cache) {
        if (cache) {
            synchronized (options) {
                Object cached = options.get(option);

                if (cached != null) {
                    return cached;
                }
            }
        }
        File file = Config.getFile();

        try {
            if (file.exists() || file.createNewFile()) {
                String key = this.hackType + "." + option;
                boolean isDefaultNull = def == null;
                YamlConfiguration configuration = Config.getConfiguration();

                if (configuration != null) {
                    if (cache) {
                        synchronized (options) {
                            if (configuration.contains(key)) {
                                Object value = configuration.get(key, def);

                                if (!isDefaultNull) {
                                    options.put(option, value);
                                }
                                return value;
                            }
                            if (!isDefaultNull) {
                                configuration.set(key, def);

                                try {
                                    configuration.save(file);
                                    options.put(option, def);
                                } catch (Exception ex) {
                                    AwarenessNotifications.forcefullySend("Failed to store '" + key + "' option in '" + file.getName() + "' file.");
                                    ex.printStackTrace();
                                }
                            }
                        }
                    } else {
                        if (configuration.contains(key)) {
                            return configuration.get(key, def);
                        } else if (!isDefaultNull) {
                            configuration.set(key, def);

                            try {
                                configuration.save(file);
                            } catch (Exception ex) {
                                AwarenessNotifications.forcefullySend("Failed to store '" + key + "' option in '" + file.getName() + "' file.");
                                ex.printStackTrace();
                            }
                        }
                    }
                } else {
                    AwarenessNotifications.forcefullySend("Failed to load checks configuration (2).");
                }
            } else {
                AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
            }
        } catch (Exception ex) {
            AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
            ex.printStackTrace();
        }
        return def;
    }

    public String getTextOption(String option, boolean def) {
        return getOption(option, def, true).toString();
    }

    public boolean getBooleanOption(String option, Boolean def) {
        Object object = getOption(option, def, true);
        return object instanceof Boolean ? (boolean) object :
                object instanceof String ? Boolean.parseBoolean(object.toString().toLowerCase()) :
                        object instanceof Long || object instanceof Integer || object instanceof Short ? ((long) object) > 0L :
                                object instanceof Double || object instanceof Float ? ((double) object) > 0.0 :
                                        def != null && def;
    }

    public int getNumericalOption(String option, int def) {
        Object object = getOption(option, def, true);

        if (object instanceof Integer || object instanceof Short) {
            return (int) object;
        } else if (object instanceof String || object instanceof Long) {
            try {
                return Integer.parseInt(object.toString());
            } catch (Exception ex) {
                return def;
            }
        } else if (object instanceof Double || object instanceof Float) {
            try {
                return AlgebraUtils.integerRound(Double.parseDouble(object.toString()));
            } catch (Exception ex) {
                return def;
            }
        } else {
            return def;
        }
    }

    public double getDecimalOption(String option, double def) {
        Object object = getOption(option, def, true);

        if (object instanceof Double || object instanceof Float) {
            return (double) object;
        } else if (object instanceof Long) {
            return ((Long) object).doubleValue();
        } else if (object instanceof Integer) {
            return ((Integer) object).doubleValue();
        } else if (object instanceof Short) {
            return ((Short) object).doubleValue();
        } else if (object instanceof String) {
            try {
                return Double.parseDouble(object.toString());
            } catch (Exception ex) {
                return def;
            }
        } else {
            return def;
        }
    }

    // Separator

    public boolean isSilent(String world) {
        return !supportsSilent
                || silent
                || world != null && isSilentOnWorld(world);
    }

    public boolean isSilentOnWorld(String world) {
        if (silentWorlds.length > 0) {
            world = world.toLowerCase();

            for (String silentWorld : silentWorlds) {
                if (silentWorld.equals(world)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[] getSilentWorlds() {
        return silentWorlds;
    }

    public boolean setSilent(boolean b) {
        if (supportsSilent && silent != b) {
            if (Config.settings.getBoolean("Important.enable_developer_api")) {
                CheckSilentToggleEvent event = new CheckSilentToggleEvent(this.hackType, Enums.ToggleAction.DISABLE);
                Register.manager.callEvent(event);

                if (event.isCancelled()) {
                    return false;
                }
            }
            this.silent = b;
            return setOption("silent", b);
        } else {
            return false;
        }
    }

    // Separator

    public void setIgnoredViolations(Enums.DataType dataType, Map<Integer, Double> map) {
        synchronized (ignoredViolations) {
            for (Map.Entry<Integer, Double> entry : map.entrySet()) {
                ignoredViolations.computeIfAbsent(
                        dataType,
                        k -> new LinkedHashMap<>()
                ).put(
                        entry.getKey(),
                        AlgebraUtils.integerCeil(entry.getValue()) // Ceil to be the safest
                );
            }
        }
    }

    public void clearIgnoredViolations() {
        synchronized (ignoredViolations) {
            ignoredViolations.clear();
        }
    }

    public Map<Enums.DataType, Map<Integer, Integer>> copyIgnoredViolations() {
        return ignoredViolations;
    }

    public int getIgnoredViolations(Enums.DataType dataType, int hash) {
        synchronized (ignoredViolations) {
            Map<Integer, Integer> map = ignoredViolations.get(dataType);

            if (map == null) {
                return standardIgnoredViolations;
            } else {
                Integer integer = map.get(hash);
                return integer == null
                        ? standardIgnoredViolations
                        : Math.max(integer, standardIgnoredViolations);
            }
        }
    }

}
