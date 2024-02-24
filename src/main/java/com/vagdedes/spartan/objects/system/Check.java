package com.vagdedes.spartan.objects.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.profiling.PlayerEvidence;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.api.CheckSilentToggleEvent;
import me.vagdedes.spartan.api.CheckToggleEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Check {

    // Static

    public static final int
            violationCycleSeconds = 60,
            violationCycleTicks = TPS.tickTimeInteger * violationCycleSeconds,
            maxCommands = 10,
            maxMath = 10,
            maxViolationsPerCycle = 100,
            minimumDefaultCancelViolation = 1,
            maximumDefaultCancelViolation = 6;
    public static final long violationCycleMilliseconds = violationCycleSeconds * 1_000L;

    private static boolean canPunishByDefault(Enums.HackType hackType) {
        return hackType != Enums.HackType.GhostHand;
    }

    private static boolean supportsLiveEvidence(Enums.HackType hackType) {
        return hackType != Enums.HackType.XRay;
    }

    private static boolean supportsSilent(Enums.HackType hackType) {
        return hackType != Enums.HackType.AutoRespawn;
    }

    // Object

    private final Enums.HackType hackType;
    private String name;
    private final Enums.CheckType checkType;
    private final Map<Integer, Collection<String>> commandsLegacy;
    private final Map<String, Object> options;
    private final int cancelViolation;
    private boolean silent;
    private final boolean handleCancelledEvents;
    private final Map<Integer, Integer> maxCancelledViolations;
    private final boolean[] enabled;
    private final boolean
            canPunish, canPunishByDefault,
            supportsLiveEvidence,
            supportsSilent,
            canBeAsync;
    private final String[]
            disabledWorlds, silentWorlds,
            description;

    // Object Methods

    public Check(Enums.HackType hackType) {
        this(hackType, new LinkedHashMap<>(), false);
    }

    public Check(Enums.HackType hackType, Map<Integer, Integer> maxCancelledViolations, boolean copy) {
        switch (hackType) {
            case FastBow:
            case FastHeal:
            case ItemDrops:
            case BlockReach:
            case AutoRespawn:
            case XRay:
            case InventoryClicks:
                this.cancelViolation = minimumDefaultCancelViolation;
                break;
            case Criticals:
            case FastPlace:
            case FastBreak:
            case FastEat:
            case NoSwing:
            case Velocity:
            case ImpossibleInventory:
            case ImpossibleActions:
                this.cancelViolation = 2;
                break;
            case KillAura:
            case FastClicks:
            case NoFall:
            case NoSlowdown:
            case HitReach:
                this.cancelViolation = 3;
                break;
            case Exploits:
            case MorePackets:
                this.cancelViolation = 4;
                break;
            case Speed:
            case IrregularMovements:
                this.cancelViolation = 5;
                break;
            case GhostHand:
                this.cancelViolation = maximumDefaultCancelViolation;
                break;
            default:
                this.cancelViolation = 0;
                break;
        }

        // Separator

        switch (hackType) {
            case KillAura:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to have an 'apparent'",
                        "combat advantage against any entity."};
                break;
            case Exploits:
                this.description = new String[]{"This check will prevent client",
                        "modules that may potentially hurt",
                        "a server's functional performance."};
                break;
            case HitReach:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to hit entities",
                        "from an abnormally long distance"};
                break;
            case Velocity:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to receive abnormal",
                        "amounts of knockback, or none at all."};
                break;
            case Speed:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to travel faster",
                        "than what is physically allowed."};
                break;
            case NoSwing:
                this.description = new String[]{"This check will prevent client modules",
                        "that manipulate packets and prevent",
                        "interaction animations from being shown."};
                break;
            case IrregularMovements:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to move abnormally,",
                        "such as stepping blocks or climbing walls."};
                break;
            case NoFall:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to decrease or",
                        "eliminate falling damage."};
                break;
            case GhostHand:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to interact or break",
                        "blocks through walls of blocks."};
                break;
            case BlockReach:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to build or break",
                        "blocks within an abnormally long distance."};
                break;
            case FastBreak:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to break one or multiple",
                        "blocks irregularly fast."};
                break;
            case FastClicks:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to click abnormally fast",
                        "or have an irregular clicking consistency."};
                break;
            case Criticals:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to critical damage",
                        "an entity without properly moving."};
                break;
            case MorePackets:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to send abnormally",
                        "high amounts of movement packets."};
                break;
            case ImpossibleActions:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to execute actions",
                        "in abnormal cases, such as when sleeping."};
                break;
            case FastPlace:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to place blocks",
                        "in abnormally fast rates."};
                break;
            case NoSlowdown:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to travel normally",
                        "while executing eating animations, or",
                        "passing through cobweb blocks."};
                break;
            case AutoRespawn:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to respawn faster",
                        "than what is physically expected."};
                break;
            case FastBow:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to shoot arrows",
                        "in abnormally fast rates."};
                break;
            case FastEat:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to consume an amount",
                        "of food in an abnormal amount of time."};
                break;
            case FastHeal:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to heal faster",
                        "than what is physically allowed."};
                break;
            case ItemDrops:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to drop an amount",
                        "of items in abnormally fast rates."};
                break;
            case InventoryClicks:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to interact with an",
                        "amount of items, in abnormally fast rates."};
                break;
            case ImpossibleInventory:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to interact with",
                        "an inventory in abnormal cases, such",
                        "as when sprinting or walking."};
                break;
            case XRay:
                this.description = new String[]{"This check will prevent client modules",
                        "that allow a player to see through blocks",
                        "in order to find rare ores, such as diamonds,",
                        "gold, and even emerald. (Logs must be enabled)"};
                break;
            default:
                this.description = new String[]{};
                break;
        }

        // Separator

        switch (hackType) {
            case KillAura:
            case Criticals:
            case FastBow:
            case FastClicks:
            case HitReach:
            case Velocity:
                this.checkType = Enums.CheckType.COMBAT;
                break;
            case Exploits:
                this.checkType = Enums.CheckType.EXPLOITS;
                break;
            case ImpossibleInventory:
            case InventoryClicks:
            case ItemDrops:
                this.checkType = Enums.CheckType.INVENTORY;
                break;
            case AutoRespawn:
            case FastEat:
            case FastHeal:
            case NoSwing:
                this.checkType = Enums.CheckType.PLAYER;
                break;
            case Speed:
            case IrregularMovements:
            case MorePackets:
            case NoSlowdown:
            case NoFall:
                this.checkType = Enums.CheckType.MOVEMENT;
                break;
            case FastPlace:
            case FastBreak:
            case BlockReach:
            case GhostHand:
            case XRay:
            case ImpossibleActions:
                this.checkType = Enums.CheckType.WORLD;
                break;
            default:
                this.checkType = null;
                break;
        }

        // Separator

        switch (hackType) { // MorePackets: Repeats too much and is not particularly heavy
            case Speed:
            case IrregularMovements:
            case NoFall:
            case KillAura:
                this.canBeAsync = true;
                break;
            default:
                this.canBeAsync = false;
                break;
        }

        // Separator

        this.options = Collections.synchronizedMap(new LinkedHashMap<>());
        this.hackType = hackType;
        this.commandsLegacy = Collections.synchronizedMap(new LinkedHashMap<>(maxViolationsPerCycle));
        this.maxCancelledViolations = copy ? maxCancelledViolations : Collections.synchronizedMap(maxCancelledViolations);

        // Separator

        boolean legacy = Config.isLegacy();
        Object silent,
                handleCancelledEvents = getOption("cancelled_event", false, false);

        if (supportsSilent(hackType)) { // Can Be Silent
            silent = getOption("silent", false, false);
        } else {
            silent = null;
        }
        String name = getOption("name", this.hackType.toString(), false).toString(),
                worlds_config = getOption("disabled_worlds", "exampleDisabledWorld1, exampleDisabledWorld2", false).toString(),
                silents_config = getOption("silent_worlds", "exampleSilentWorld1, exampleSilentWorld2", false).toString();

        // Separator
        ResearchEngine.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);
        this.enabled = new boolean[ResearchEngine.usableDataTypes.length];

        if (legacy) {
            Object optionValue = getOption("enabled", true, false);
            boolean boolValue = optionValue instanceof Boolean ? (boolean) optionValue :
                    optionValue instanceof Long || optionValue instanceof Integer || optionValue instanceof Short ? ((long) optionValue) > 0L :
                            optionValue instanceof Double || optionValue instanceof Float ? ((double) optionValue) > 0.0 :
                                    Boolean.parseBoolean(optionValue.toString().toLowerCase());

            for (ResearchEngine.DataType dataType : dataTypes) {
                this.enabled[dataType.ordinal()] = boolValue;
            }
        } else {
            Object oldOptionValue = getOption("enabled", null, false);
            boolean hasOldOption = oldOptionValue instanceof Boolean;

            if (hasOldOption) {
                setOption("enabled", null, false);
            }

            for (ResearchEngine.DataType dataType : dataTypes) {
                Object optionValue = getOption(
                        "enabled." + dataType.lowerCase,
                        hasOldOption ? oldOptionValue : true,
                        false
                );
                this.enabled[dataType.ordinal()] = optionValue instanceof Boolean ? (boolean) optionValue :
                        optionValue instanceof Long || optionValue instanceof Integer || optionValue instanceof Short ? ((long) optionValue) > 0L :
                                optionValue instanceof Double || optionValue instanceof Float ? ((double) optionValue) > 0.0 :
                                        Boolean.parseBoolean(optionValue.toString().toLowerCase());
            }
        }
        this.supportsLiveEvidence = supportsLiveEvidence(hackType);

        if (this.supportsLiveEvidence) {
            this.canPunishByDefault = canPunishByDefault(hackType);

            if (legacy) {
                ConfigurationSection section = Register.plugin.getConfig().getConfigurationSection(hackType + ".punishments");
                this.canPunish = section != null && !section.getKeys(false).isEmpty();

                if (!this.canPunish && this.canPunishByDefault) {
                    File file = Config.getFile();

                    try {
                        if (file.exists() || file.createNewFile()) {
                            for (int position = 0; position < Check.maxCommands; position++) {
                                ConfigUtils.add(
                                        file,
                                        hackType + ".punishments." + AlgebraUtils.integerRound(this.cancelViolation * PlayerEvidence.standardRatio) + "." + (position + 1),
                                        Config.settings.getDefaultPunishmentCommands().get(position)
                                );
                            }
                        } else {
                            AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
                        }
                    } catch (Exception ex) {
                        AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
                        ex.printStackTrace();
                    }
                }
            } else {
                Object punish = getOption("punish", this.canPunishByDefault, false);
                this.canPunish = punish instanceof Boolean ? (boolean) punish :
                        punish instanceof Long || punish instanceof Integer || punish instanceof Short ? ((long) punish) > 0L :
                                punish instanceof Double || punish instanceof Float ? ((double) punish) > 0.0 :
                                        Boolean.parseBoolean(punish.toString().toLowerCase());
            }
        } else {
            this.canPunish = false;
            this.canPunishByDefault = false;
        }

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
        synchronized (commandsLegacy) {
            commandsLegacy.clear(); // Always clear regardless
        }
        synchronized (options) {
            options.clear();
        }
    }

    // Separator

    public boolean isEnabled(ResearchEngine.DataType dataType, String world, SpartanPlayer player) {
        ResearchEngine.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

        if (dataType == null) {
            for (ResearchEngine.DataType type : dataTypes) {
                if (this.enabled[type.ordinal()]) {
                    return true;
                }
            }
            return false;
        } else if (!this.enabled[dataType.ordinal()]) {
            return false;
        }
        return (world == null || isEnabledOnWorld(world))
                && (player == null || player.getViolations(hackType).getDisableCause() == null);
    }

    public void setEnabled(ResearchEngine.DataType dataType, boolean b) {
        ResearchEngine.DataType[] dataTypes;

        if (dataType == null) {
            dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);
        } else {
            dataTypes = null;

            for (ResearchEngine.DataType type : ResearchEngine.getDynamicUsableDataTypes(false)) {
                if (type == dataType) {
                    dataTypes = new ResearchEngine.DataType[]{dataType};
                    break;
                }
            }

            if (dataTypes == null) {
                return;
            }
        }
        for (ResearchEngine.DataType type : dataTypes) {
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

                    if (b) {
                        CloudFeature.refresh(true);
                    } else {
                        clearConfigurationCache();

                        for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
                            player.getViolations(hackType).reset();
                        }
                    }
                    setOption("enabled." + type.lowerCase, b, false);
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

    public String[] getDescription() {
        return description;
    }

    // Separator

    public boolean canBeAsynchronous() {
        return canBeAsync;
    }

    public int ordinal() {
        return hackType.hashCode();
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

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
        setOption("name", name, false);
    }

    // Separator

    public boolean setOption(String option, Object value) {
        return setOption(option, value, true);
    }

    private boolean setOption(String option, Object value, boolean production) {
        File file = Config.getFile();

        try {
            if (file.exists() || file.createNewFile()) {
                String key = this.hackType + "." + option;
                YamlConfiguration configuration = Config.getConfiguration();

                if (configuration != null) {
                    configuration.set(key, value);

                    try {
                        configuration.save(file);

                        if (Config.isLegacy()) {
                            Register.plugin.reloadConfig();
                        }
                        if (production) {
                            this.hackType.resetCheck();
                        } else {
                            options.remove(key); // Remove instead of modifying to be on demand and have the chance to catch changes by the user
                        }
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
        if (TestServer.isIdentified()) {
            return def;
        }
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

                                    if (Config.isLegacy()) {
                                        Register.plugin.reloadConfig();
                                    }
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

                                if (Config.isLegacy()) {
                                    Register.plugin.reloadConfig();
                                }
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

    public boolean supportsSilent() {
        return supportsSilent;
    }

    public boolean isSilent(String world) {
        return !supportsSilent()
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

    public void setSilent(boolean b) {
        if (supportsSilent() && silent != b) {
            if (Config.settings.getBoolean("Important.enable_developer_api")) {
                CheckSilentToggleEvent event = new CheckSilentToggleEvent(this.hackType, Enums.ToggleAction.DISABLE);
                Register.manager.callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
            }
            this.silent = b;
            setOption("silent", b, false);
        }
    }

    // Separator

    public Enums.CheckType getCheckType() {
        return checkType;
    }

    // Separator

    public boolean canHandleCancelledEvents() {
        return handleCancelledEvents;
    }

    // Separator

    public boolean canPunishByDefault() {
        return canPunishByDefault;
    }

    public boolean supportsLiveEvidence() {
        return supportsLiveEvidence;
    }

    public boolean canPunish() {
        return canPunish;
    }

    // Separator

    public boolean hasMinimumDefaultCancelViolation() {
        return cancelViolation == minimumDefaultCancelViolation;
    }

    public boolean hasMaximumDefaultCancelViolation() {
        return cancelViolation == maximumDefaultCancelViolation;
    }

    // Separator

    public int getProblematicDetections() {
        if (!maxCancelledViolations.isEmpty()) {
            synchronized (maxCancelledViolations) {
                int count = 0;

                for (int level : maxCancelledViolations.values()) {
                    if (level >= cancelViolation) {
                        count++;
                    }
                }
                return count;
            }
        } else {
            return 0;
        }
    }

    public void setMaxCancelledViolations(ResearchEngine.DataType dataType, Map<Integer, Double> map) {
        synchronized (maxCancelledViolations) {
            if (!map.isEmpty()) {
                for (Map.Entry<Integer, Double> entry : map.entrySet()) {
                    maxCancelledViolations.put(
                            (dataType.hashCode() * SpartanBukkit.hashCodeMultiplier) + entry.getKey(),
                            AlgebraUtils.integerCeil(entry.getValue()) // Ceil to be the safest
                    );
                }
            }
        }
    }

    public void clearMaxCancelledViolations() {
        synchronized (maxCancelledViolations) {
            maxCancelledViolations.clear();
        }
    }

    public Map<Integer, Integer> copyMaxCancelledViolations() {
        return maxCancelledViolations;
    }

    public int getMaxCancelledViolations(ResearchEngine.DataType dataType, int hash) {
        synchronized (maxCancelledViolations) {
            return maxCancelledViolations.getOrDefault(
                    (dataType.hashCode() * SpartanBukkit.hashCodeMultiplier) + hash,
                    0
            );
        }
    }

    // Separator

    public int getCancelViolation() {
        return cancelViolation;
    }

    public Collection<String> getLegacyCommands(int violation) {
        synchronized (commandsLegacy) {
            // Cache
            Collection<String> commandsFound = commandsLegacy.get(violation);

            if (commandsFound != null) {
                return new ArrayList<>(commandsFound);
            }
            // Base Variables
            FileConfiguration config = Register.plugin.getConfig();
            commandsFound = new ArrayList<>();
            String punishmentKey = hackType + ".punishments.";

            // Numbers Handler
            boolean containsNumber = config.contains(punishmentKey + violation);
            Set<Integer> mathFound = new HashSet<>(maxMath);

            for (int math = 1; math <= maxMath; math++) {
                if (math > 1 && config.contains(punishmentKey + violation + "*" + math) || config.contains(punishmentKey + violation + "+" + math)) {
                    mathFound.add(math);
                }
            }
            boolean containsMath = !mathFound.isEmpty();

            // Search Handler
            if (containsNumber || containsMath) {
                for (int command = 1; command <= maxCommands; command++) {
                    boolean multiply = false;
                    String punishmentKeyModifiable = punishmentKey + violation;
                    int mathKey = 0;

                    if (containsMath) {
                        for (int math : mathFound) {
                            String key = punishmentKeyModifiable + "*" + math + "." + command;

                            if (config.contains(key)) {
                                multiply = true;
                                mathKey = math;
                                punishmentKeyModifiable = key;
                                break;
                            } else {
                                key = punishmentKeyModifiable + "+" + math + "." + command;

                                if (config.contains(key)) {
                                    mathKey = math;
                                    punishmentKeyModifiable = key;
                                    break;
                                }
                            }
                        }
                    }
                    if (mathKey == 0) {
                        punishmentKeyModifiable = punishmentKeyModifiable + "." + command;
                    }

                    if (config.contains(punishmentKeyModifiable)) {
                        String commandContent = config.getString(punishmentKeyModifiable);

                        if (commandContent != null && !commandContent.isEmpty()) {
                            commandsFound.add(commandContent);
                            commandsLegacy.put(violation, commandsFound);

                            if (mathKey > 0 && violation > 0) {
                                int violationModifiable = violation;

                                while (violationModifiable <= maxViolationsPerCycle) {

                                    if (multiply) {
                                        violationModifiable *= mathKey;
                                    } else {
                                        violationModifiable += mathKey;
                                    }
                                    commandsLegacy.putIfAbsent(violationModifiable, commandsFound);
                                }
                            }
                        }
                    }
                }
            }

            // Empty Handler
            if (commandsFound.isEmpty()) {
                commandsLegacy.put(violation, new LinkedList<>());
            }

            // Return List
            return new ArrayList<>(commandsFound);
        }
    }
}
