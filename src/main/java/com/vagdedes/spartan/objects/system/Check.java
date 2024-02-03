package com.vagdedes.spartan.objects.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.api.CheckSilentToggleEvent;
import me.vagdedes.spartan.api.CheckToggleEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Check {

    // Static Values

    public static final long
            violationCycleSeconds = 60_000L,
            recentViolationSeconds = 10_000L;

    public static final int
            maxCommands = 10,
            maxMath = 10,
            maxViolationsPerCycle = 100,
            sufficientViolations = (maxViolationsPerCycle / 10),
            minimumDefaultCancelViolation = 1,
            maximumDefaultCancelViolation = 6,
            hackerCheckAmount = 3;

    public static final Enums.PunishmentCategory analysisMultiplierCategory = Enums.PunishmentCategory.CERTAIN;
    public static final double analysisMultiplier = analysisMultiplierCategory.getMultiplier();

    // Static Methods

    public static int getCategoryPunishment(int cancelViolation, Enums.PunishmentCategory category) {
        return AlgebraUtils.integerRound(cancelViolation * category.getMultiplier());
    }

    public static int getCategoryPunishment(Enums.HackType hackType, ResearchEngine.DataType dataType, Enums.PunishmentCategory category) {
        return getCategoryPunishment(CancelViolation.get(hackType, dataType), category);
    }

    public static Enums.PunishmentCategory getCategoryFromViolations(int violations, Enums.HackType hackType, boolean suspectedOrHacker) {
        if (!suspectedOrHacker) {
            int cancelViolation = hackType.getCheck().getDefaultCancelViolation();

            for (Enums.PunishmentCategory category : Enums.PunishmentCategory.values()) {
                if (violations <= (cancelViolation * category.getMultiplier())) {
                    return category;
                }
            }
        }
        return Enums.PunishmentCategory.ABSOLUTE;
    }

    // Separator

    private static boolean canPunishByDefault(Enums.HackType hackType) {
        return hackType != Enums.HackType.GhostHand;
    }

    private static boolean supportsLiveEvidence(Enums.HackType hackType) {
        return hackType != Enums.HackType.XRay;
    }

    // Separator

    public static boolean isUsingCustomCheckNames() {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (!hackType.getCheck().getName().equals(hackType.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUsingPerWorldFeatures() {
        List<World> worlds = Bukkit.getWorlds();

        if (!worlds.isEmpty()) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                Check check = hackType.getCheck();

                for (World worldObject : worlds) {
                    String world = worldObject.getName();

                    if (!check.isEnabledOnWorld(world)
                            || check.isSilentOnWorld(world)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Object Values

    private final Enums.HackType hackType;
    private String name;
    private final Enums.CheckType checkType;
    private final Map<Integer, List<String>> commandsLegacy;
    private final Map<String, Object> options;
    private short silent;
    private int cancelViolation;
    private final int defaultCancelViolation;
    private boolean hasCancelViolation;
    private final Map<Integer, Integer> maxCancelledViolations;
    private final boolean[] enabled;
    private final boolean
            canPunish, canPunishByDefault,
            supportsLiveEvidence,
            canBeSilent,
            canBeAsync;
    private final String[]
            disabledWorlds, silentWorlds,
            commands,
            description;
    private final Map<UUID, CancelCause>
            disabledUsers,
            silentUsers;

    // Object Methods

    public Check(Enums.HackType hackType) {
        this(
                hackType,
                new LinkedHashMap<>(Config.getMaxPlayers()), // Not concurrent as it is accessed specifically and rapidly
                new LinkedHashMap<>(Config.getMaxPlayers()), // Not concurrent as it is accessed specifically and rapidly
                new LinkedHashMap<>() // Not concurrent as it is accessed specifically and rapidly
        );
    }

    public Check(Enums.HackType hackType,
                 Map<UUID, CancelCause> disabledUsers,
                 Map<UUID, CancelCause> silentUsers,
                 Map<Integer, Integer> maxCancelledViolations) {
        switch (hackType) {
            case FastBow:
            case FastHeal:
            case ItemDrops:
            case BlockReach:
            case AutoRespawn:
            case XRay:
            case InventoryClicks:
                this.defaultCancelViolation = minimumDefaultCancelViolation;
                break;
            case Criticals:
            case FastPlace:
            case FastBreak:
            case FastEat:
            case NoSwing:
            case Velocity:
            case ImpossibleInventory:
            case ImpossibleActions:
                this.defaultCancelViolation = 2;
                break;
            case KillAura:
            case FastClicks:
            case NoFall:
            case NoSlowdown:
            case HitReach:
                this.defaultCancelViolation = 3;
                break;
            case Exploits:
            case MorePackets:
                this.defaultCancelViolation = 4;
                break;
            case Speed:
            case IrregularMovements:
                this.defaultCancelViolation = 5;
                break;
            case GhostHand:
                this.defaultCancelViolation = maximumDefaultCancelViolation;
                break;
            default:
                this.defaultCancelViolation = 0;
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

        this.options = new ConcurrentHashMap<>();
        this.hackType = hackType;
        this.commandsLegacy = new LinkedHashMap<>(maxViolationsPerCycle);
        this.disabledUsers = disabledUsers;
        this.silentUsers = silentUsers;
        this.maxCancelledViolations = maxCancelledViolations;

        // Separator

        boolean legacy = Config.isLegacy();
        List<String> commands = new ArrayList<>(maxCommands);
        commands.add("spartan {player} if {tps} > 18.0 do spartan kick {player} {detections}");

        for (int position = (commands.size() + 1); position <= maxCommands; position++) {
            commands.add("");
        }
        Object silent;

        if (hackType != Enums.HackType.AutoRespawn) { // Can Be Silent
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
                this.commands = new String[]{};
                ConfigurationSection section = Register.plugin.getConfig().getConfigurationSection(hackType + ".punishments");
                this.canPunish = section != null && !section.getKeys(false).isEmpty();

                if (!this.canPunish && this.canPunishByDefault) {
                    File file = Config.getFile();

                    try {
                        if (file.exists() || file.createNewFile()) {
                            for (int position = 1; position <= commands.size(); position++) {
                                ConfigUtils.add(file, hackType + ".punishments." + analysisMultiplierCategory.toString() + "." + position, commands.get(position - 1));
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
                File file = Config.settings.getFile();
                String[] commandsAfterException = new String[]{};

                try {
                    if (file.exists() || file.createNewFile()) {
                        for (int position = 1; position <= commands.size(); position++) {
                            ConfigUtils.add(file, "Punishments.Commands." + position, commands.get(position - 1));
                        }
                        List<String> list = Config.settings.getPunishments();

                        if (!list.isEmpty()) {
                            commandsAfterException = list.toArray(new String[0]);
                        }
                    } else {
                        AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
                    }
                } catch (Exception ex) {
                    AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
                    ex.printStackTrace();
                }
                Object punish = getOption("punish", this.canPunishByDefault, false);
                this.canPunish = punish instanceof Boolean ? (boolean) punish :
                        punish instanceof Long || punish instanceof Integer || punish instanceof Short ? ((long) punish) > 0L :
                                punish instanceof Double || punish instanceof Float ? ((double) punish) > 0.0 :
                                        Boolean.parseBoolean(punish.toString().toLowerCase());
                this.commands = commandsAfterException;
            }
        } else {
            this.canPunish = false;
            this.canPunishByDefault = false;
            this.commands = new String[]{};
        }

        // Separator

        if (name != null) {
            this.name = name;
        } else {
            this.name = hackType.toString();
        }

        // Separator

        if (silent != null) {
            this.canBeSilent = true;

            if (silent instanceof String && silent.toString().equalsIgnoreCase("dynamic")) {
                this.silent = 2;
            } else if (silent instanceof Boolean) {
                if ((boolean) silent) {
                    this.silent = 1;
                } else {
                    this.silent = 0;
                }
            } else if (silent instanceof Long || silent instanceof Integer || silent instanceof Short) {
                this.silent = (short) (((long) silent) > 0L ? 1 : 0);
            } else if (silent instanceof Double || silent instanceof Float) {
                this.silent = (short) (((double) silent) > 0.0 ? 1 : 0);
            } else {
                this.silent = (short) (Boolean.parseBoolean(silent.toString().toLowerCase()) ? 1 : 0);
            }
        } else {
            this.canBeSilent = false;
            this.silent = 0;
        }

        // Separator

        if (canBeSilent) {
            Object cancelAfterViolation = getOption("cancel_after_violation", null, false);

            if (cancelAfterViolation instanceof Integer || cancelAfterViolation instanceof Short) {
                this.hasCancelViolation = true;
                int cancelViolation = (int) cancelAfterViolation;

                if (cancelViolation > maxViolationsPerCycle) {
                    this.silent = 1;
                    this.cancelViolation = maxViolationsPerCycle;
                } else {
                    this.cancelViolation = Math.max(1, cancelViolation);
                }
            } else if (cancelAfterViolation instanceof String || cancelAfterViolation instanceof Long) {
                try {
                    int cancelViolation = Integer.parseInt(cancelAfterViolation.toString());

                    if (cancelViolation > maxViolationsPerCycle) {
                        this.silent = 1;
                        this.cancelViolation = maxViolationsPerCycle;
                    } else {
                        this.cancelViolation = Math.max(1, cancelViolation);
                    }
                    this.hasCancelViolation = true; // Always at the end to run after the critical code
                } catch (Exception ex) {
                    this.cancelViolation = defaultCancelViolation;
                    this.hasCancelViolation = false;
                }
            } else if (cancelAfterViolation instanceof Double || cancelAfterViolation instanceof Float) {
                try {
                    int cancelViolation = AlgebraUtils.integerRound((double) cancelAfterViolation);

                    if (cancelViolation > maxViolationsPerCycle) {
                        this.silent = 1;
                        this.cancelViolation = maxViolationsPerCycle;
                    } else {
                        this.cancelViolation = Math.max(1, cancelViolation);
                    }
                    this.hasCancelViolation = true; // Always at the end to run after the critical code
                } catch (Exception ex) {
                    this.cancelViolation = defaultCancelViolation;
                    this.hasCancelViolation = false;
                }
            } else {
                this.cancelViolation = defaultCancelViolation;
                this.hasCancelViolation = false;
            }
        } else {
            this.cancelViolation = Integer.MIN_VALUE;
            this.hasCancelViolation = false;
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

        if (canBeSilent && silents_config != null) {
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
        commandsLegacy.clear(); // Always clear regardless
        options.clear();
    }

    public void clearCancelCauses() {
        synchronized (disabledUsers) {
            disabledUsers.clear();
        }
        synchronized (silentUsers) {
            silentUsers.clear();
        }
    }

    public void clearCache() {
        clearConfigurationCache();
        clearCancelCauses();
        clearMaxCancelledViolations();
    }

    // Separator

    public Map<UUID, CancelCause> copyDisabledUsers() {
        synchronized (disabledUsers) {
            return new HashMap<>(disabledUsers);
        }
    }

    public CancelCause getDisabledCause(UUID uuid) {
        synchronized (disabledUsers) {
            CancelCause cancelCause = disabledUsers.get(uuid);
            return cancelCause != null && !cancelCause.hasExpired() ? cancelCause : null;
        }
    }

    public void addDisabledUser(UUID uuid, String reason, String pointer, int ticks) {
        synchronized (disabledUsers) {
            CancelCause cancelCause = disabledUsers.get(uuid);

            if (cancelCause != null) {
                cancelCause.merge(new CancelCause(reason, pointer, ticks));
            } else if (SpartanBukkit.isPlayer(uuid)) {
                disabledUsers.put(uuid, new CancelCause(reason, pointer, ticks));
            }
            SpartanMenu.playerInfo.refresh(uuid);
        }
    }

    public void addDisabledUser(UUID uuid, String reason, int ticks) {
        addDisabledUser(uuid, reason, null, ticks);
    }

    public void removeDisabledUser(UUID uuid) {
        synchronized (disabledUsers) {
            if (disabledUsers.remove(uuid) != null) {
                SpartanMenu.playerInfo.refresh(uuid);
            }
        }
    }

    public boolean isEnabled(ResearchEngine.DataType dataType, String world, UUID player) {
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
                && (player == null || getDisabledCause(player) == null);
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
                        clearCancelCauses();

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

    public Set<String> getOptionKeys() {
        return options.keySet();
    }

    public Collection<Object> getOptionValues() {
        return options.values();
    }

    public Set<Map.Entry<String, Object>> getOptions() {
        return options.entrySet();
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
            Object cached = options.get(option);

            if (cached != null) {
                return cached;
            }
        }
        File file = Config.getFile();

        try {
            if (file.exists() || file.createNewFile()) {
                String key = this.hackType + "." + option;
                boolean isDefaultNull = def == null;
                YamlConfiguration configuration = Config.getConfiguration();

                if (configuration != null) {
                    if (configuration.contains(key)) {
                        Object value = configuration.get(key, def);

                        if (cache && !isDefaultNull) {
                            options.put(option, value);
                        }
                        return value;
                    }
                    if (!isDefaultNull) {
                        configuration.set(key, def);

                        try {
                            configuration.save(file);

                            if (cache) {
                                options.put(option, def);
                            }
                            if (Config.isLegacy()) {
                                Register.plugin.reloadConfig();
                            }
                        } catch (Exception ex) {
                            AwarenessNotifications.forcefullySend("Failed to store '" + key + "' option in '" + file.getName() + "' file.");
                            ex.printStackTrace();
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

    public Map<UUID, CancelCause> copySilentUsers() {
        synchronized (silentUsers) {
            return new HashMap<>(silentUsers);
        }
    }

    public CancelCause getSilentCause(UUID uuid) {
        synchronized (silentUsers) {
            CancelCause cancelCause = silentUsers.get(uuid);
            return cancelCause != null && !cancelCause.hasExpired() ? cancelCause : null;
        }
    }

    public void addSilentUser(UUID uuid, String reason, String pointer, int ticks) {
        synchronized (silentUsers) {
            CancelCause cancelCause = silentUsers.get(uuid);

            if (cancelCause != null) {
                cancelCause.merge(new CancelCause(reason, pointer, ticks));
            } else if (SpartanBukkit.isPlayer(uuid)) {
                silentUsers.put(uuid, new CancelCause(reason, pointer, ticks));
            }
            SpartanMenu.playerInfo.refresh(uuid);
        }
    }

    public void addSilentUser(UUID uuid, String reason, int ticks) {
        addSilentUser(uuid, reason, null, ticks);
    }

    public void removeSilentUser(UUID uuid) {
        synchronized (silentUsers) {
            if (silentUsers.remove(uuid) != null) {
                SpartanMenu.playerInfo.refresh(uuid);
            }
        }
    }

    public boolean canBeSilent() {
        return canBeSilent;
    }

    public boolean isSilent(String world, UUID player) {
        return !canBeSilent()
                || silent == 1
                || silent == 2 && DetectionNotifications.getPlayersRawSize() > 0
                || world != null && isSilentOnWorld(world)
                || player != null && getSilentCause(player) != null;
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

    public void setSilent(String s) {
        if (canBeSilent()) {
            switch (s) {
                case "dynamic":
                    if (silent != 2) {
                        if (Config.settings.getBoolean("Important.enable_developer_api")) {
                            CheckSilentToggleEvent event = new CheckSilentToggleEvent(this.hackType, Enums.ToggleAction.ENABLE);
                            Register.manager.callEvent(event);

                            if (event.isCancelled()) {
                                return;
                            }
                        }
                        this.silent = 2;
                        setOption("silent", s.toLowerCase(), false);
                    }
                    break;
                case "true":
                    if (silent != 1) {
                        if (Config.settings.getBoolean("Important.enable_developer_api")) {
                            CheckSilentToggleEvent event = new CheckSilentToggleEvent(this.hackType, Enums.ToggleAction.ENABLE);
                            Register.manager.callEvent(event);

                            if (event.isCancelled()) {
                                return;
                            }
                        }
                        this.silent = 1;
                        setOption("silent", true, false);
                    }
                    break;
                case "false":
                    if (silent != 0) {
                        if (Config.settings.getBoolean("Important.enable_developer_api")) {
                            CheckSilentToggleEvent event = new CheckSilentToggleEvent(this.hackType, Enums.ToggleAction.DISABLE);
                            Register.manager.callEvent(event);

                            if (event.isCancelled()) {
                                return;
                            }
                        }
                        this.silent = 0;
                        setOption("silent", false, false);
                    }
                    break;
            }
        }
    }

    // Separator

    public Enums.CheckType getCheckType() {
        return checkType;
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

    public boolean isUsingCancelViolation(int preferred, int deviationUpwards) {
        return cancelViolation + deviationUpwards >= preferred;
    }

    public int getCancelViolation() {
        return cancelViolation;
    }

    public boolean hasCancelViolation() {
        return hasCancelViolation;
    }

    public boolean setCancelViolation(int i) {
        if (canBeSilent()) {
            Object cancelAfterViolation = getOption("cancel_after_violation", null, false);

            if (!(cancelAfterViolation instanceof Integer) || i > (int) cancelAfterViolation) {
                this.hasCancelViolation = true;
                setOption("cancel_after_violation", i, false);

                if (i > maxViolationsPerCycle) {
                    this.silent = 1;
                    this.cancelViolation = maxViolationsPerCycle;
                } else {
                    this.cancelViolation = Math.max(1, i);
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasMinimumDefaultCancelViolation() {
        return defaultCancelViolation == minimumDefaultCancelViolation;
    }

    public boolean hasMaximumDefaultCancelViolation() {
        return defaultCancelViolation == maximumDefaultCancelViolation;
    }

    // Separator

    public int getProblematicDetections() {
        if (!maxCancelledViolations.isEmpty()) {
            synchronized (maxCancelledViolations) {
                int count = 0,
                        cancelViolation = getCancelViolation();

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
        synchronized (maxCancelledViolations) {
            return new HashMap<>(maxCancelledViolations);
        }
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

    public int getDefaultCancelViolation() {
        return defaultCancelViolation;
    }

    public String[] getCommands() {
        return commands;
    }

    public String[] getLegacyCommands(int violation) {
        // Cache
        List<String> commandsFound = commandsLegacy.get(violation);

        if (commandsFound != null) {
            return commandsFound.toArray(new String[0]);
        }
        // Base Variables
        FileConfiguration config = Register.plugin.getConfig();
        commandsFound = new LinkedList<>();
        String punishmentKey = hackType + ".punishments.";

        // Categories Handler
        Enums.PunishmentCategory[] categories = Enums.PunishmentCategory.values();
        Set<Enums.PunishmentCategory> categoriesFound = new HashSet<>(categories.length);

        for (Enums.PunishmentCategory category : categories) {
            String loopKey = punishmentKey + category.toString();

            if (config.contains(loopKey)) {
                categoriesFound.add(category);
            }
        }
        boolean containsCategory = !categoriesFound.isEmpty();

        if (containsCategory) {
            int defaultCancelViolation = this.getDefaultCancelViolation();

            for (Enums.PunishmentCategory category : categoriesFound) {
                String string = category.toString();
                int violationModifiable = AlgebraUtils.integerRound(defaultCancelViolation * category.getMultiplier());

                if (!commandsLegacy.containsKey(violationModifiable)) {
                    List<String> commandsFoundModifiable = new LinkedList<>();

                    for (int command = 1; command <= maxCommands; command++) {
                        String punishmentKeyModifiable = punishmentKey + string + "." + command;

                        if (config.contains(punishmentKeyModifiable)) {
                            String commandContent = config.getString(punishmentKeyModifiable);

                            if (commandContent != null && commandContent.length() > 0) {
                                commandsFoundModifiable.add(commandContent);
                            }
                        }
                    }
                    commandsLegacy.put(violationModifiable, commandsFoundModifiable);

                    if (violation == violationModifiable) {
                        commandsFound = commandsFoundModifiable;
                    }
                }
            }
        }

        // Numbers Handler
        if (commandsFound.size() == 0) {
            boolean containsNumber = config.contains(punishmentKey + violation);
            Set<Integer> mathFound = new HashSet<>(maxMath);

            for (int math = 1; math <= maxMath; math++) {
                if (math > 1 && config.contains(punishmentKey + violation + "*" + math) || config.contains(punishmentKey + violation + "+" + math)) {
                    mathFound.add(math);
                }
            }
            boolean containsMath = mathFound.size() > 0;

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

                        if (commandContent != null && commandContent.length() > 0) {
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
        }

        // Empty Handler
        if (commandsFound.isEmpty()) {
            commandsLegacy.put(violation, new LinkedList<>());
        }

        // Return List
        return commandsFound.toArray(new String[0]);
    }
}
