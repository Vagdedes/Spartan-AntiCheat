package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.check.implementation.combat.FastBow;
import com.vagdedes.spartan.abstraction.check.implementation.combat.HitReach;
import com.vagdedes.spartan.abstraction.check.implementation.combat.VelocityCheck;
import com.vagdedes.spartan.abstraction.check.implementation.combat.criticals.Criticals;
import com.vagdedes.spartan.abstraction.check.implementation.combat.fastClicks.FastClicks;
import com.vagdedes.spartan.abstraction.check.implementation.combat.killAura.KillAura;
import com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits;
import com.vagdedes.spartan.abstraction.check.implementation.inventory.ImpossibleInventory;
import com.vagdedes.spartan.abstraction.check.implementation.inventory.InventoryClicks;
import com.vagdedes.spartan.abstraction.check.implementation.inventory.ItemDrops;
import com.vagdedes.spartan.abstraction.check.implementation.movement.MorePackets;
import com.vagdedes.spartan.abstraction.check.implementation.movement.NoFall;
import com.vagdedes.spartan.abstraction.check.implementation.movement.NoSlowdown;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.Speed;
import com.vagdedes.spartan.abstraction.check.implementation.player.AutoRespawn;
import com.vagdedes.spartan.abstraction.check.implementation.player.FastEat;
import com.vagdedes.spartan.abstraction.check.implementation.player.FastHeal;
import com.vagdedes.spartan.abstraction.check.implementation.player.NoSwing;
import com.vagdedes.spartan.abstraction.check.implementation.world.*;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.data.*;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.ElytraUse;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Velocity;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.EventsHandler7;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpartanPlayer {

    private static final int tickFrequency = 5 * 1200;
    private static int syncTicks = 0;
    private static final Map<World, List<Entity>> worldEntities
            = Collections.synchronizedMap(new LinkedHashMap<>(Math.max(Bukkit.getWorlds().size(), 1)));

    private float walkSpeed, flySpeed, fallDistance;
    private boolean frozen, onGroundCustom, dead, sleeping, usingItem, invulnerable;
    private int ping, maximumNoDamageTicks, foodLevel;
    private double eyeHeight, health;
    private GameMode gameMode;
    public final boolean bedrockPlayer;
    public final long creationTime, lastPlayed;
    public final String name;
    public final UUID uuid;
    public final String ipAddress;
    private final Collection<PotionEffect> potionEffects;
    public final Enums.DataType dataType;
    private SpartanInventory inventory;
    private SpartanOpenInventory openInventory;
    public final SpartanPlayerMovement movement;
    private final Map<EntityDamageEvent.DamageCause, SpartanPlayerDamage> damageReceived, damageDealt;
    private SpartanPlayerDamage lastDamageReceived, lastDamageDealt;

    // Data

    private final Buffer[] buffer;
    private final Timer[] timer;
    private final Decimals[] decimals;
    private final Tracker[] tracker;
    private final Cooldowns[] cooldowns;
    private final CheckExecutor[] executors;
    private final LiveViolation[] violations;
    final Handlers handlers;
    private LiveViolation lastViolation;

    // Cache

    private PlayerProfile playerProfile;
    private final Clicks clicks;

    private Entity vehicle;
    private final List<Entity> nearbyEntities;
    private double nearbyEntitiesDistance;

    private final boolean[] runChecks;
    private final Map<Enums.HackType, Boolean> runCheck;
    private final Map<Enums.HackType, Boolean> runCheckAccountLag;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (MultiVersion.folia) {
                    run(false);
                } else if (!SpartanBukkit.playerThread.executeIfFree(() -> run(false))) {
                    SpartanBukkit.detectionThread.executeIfFreeElseHere(() -> run(false));
                }
                run(true);
            }, 1L, 1L);
        }
    }

    // Separator

    public static void clear() {
        synchronized (worldEntities) {
            worldEntities.clear();
        }
        SpartanLocation.clear();
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                for (Buffer buffer : p.buffer) {
                    buffer.clear();
                }
                for (Timer timer : p.timer) {
                    timer.clear();
                }
                for (Decimals decimals : p.decimals) {
                    decimals.clear();
                }
                for (Tracker tracker : p.tracker) {
                    tracker.clear();
                }
                for (Cooldowns cooldowns : p.cooldowns) {
                    cooldowns.clear();
                }
                p.runCheck.clear();
                p.runCheckAccountLag.clear();
            }
        }
    }

    public static void cacheEntity(Entity entity) {
        synchronized (worldEntities) {
            List<Entity> collection = worldEntities.get(entity.getWorld());

            if (collection != null) {
                collection.add(entity);
            }
        }
    }

    private static void run(boolean sync) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (sync) {
            boolean clearCache, hasPlayers = !players.isEmpty();

            if (syncTicks == 0) {
                clearCache = true;
                syncTicks = tickFrequency;

                if (!MultiVersion.folia) {
                    synchronized (worldEntities) {
                        worldEntities.clear();

                        if (hasPlayers) {
                            for (World world : Bukkit.getWorlds()) {
                                worldEntities.put(
                                        world,
                                        new ArrayList<>(world.getEntities())
                                );
                            }
                        }
                    }
                }
            } else {
                clearCache = false;
                syncTicks--;

                if (!MultiVersion.folia && syncTicks % 20 == 0) {
                    synchronized (worldEntities) {
                        worldEntities.clear();

                        if (hasPlayers) {
                            for (World world : Bukkit.getWorlds()) {
                                worldEntities.put(
                                        world,
                                        new ArrayList<>(world.getEntities())
                                );
                            }
                        }
                    }
                }
            }

            if (hasPlayers) {
                for (SpartanPlayer p : players) {
                    if (clearCache) {
                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            if (!p.getViolations(hackType).hasLevel()) {
                                p.getBuffer(hackType).clear();
                                p.getTimer(hackType).clear();
                                p.getDecimals(hackType).clear();
                                p.getCooldowns(hackType).clear();
                                p.getTracker(hackType).clear();
                            }
                        }
                    }
                    p.nearbyEntitiesDistance = 0;
                    p.runCheck.clear();
                    p.runCheckAccountLag.clear();

                    // Separator
                    SpartanBukkit.runTask(p, () -> {
                        Player n = p.getPlayer();

                        if (n != null) {
                            Collection<PotionEffect> potionEffects = n.getActivePotionEffects();
                            p.setActivePotionEffects(potionEffects); // Bad
                            PlayerUtils.run(p, n, potionEffects);
                        }

                        // Preventions
                        for (Enums.HackType hackType : EventsHandler7.handledChecks) {
                            if (p.getViolations(hackType).prevent()) {
                                break;
                            }
                        }
                    });
                }
            }
        } else if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                SpartanBukkit.runTask(p, () -> {
                    if (p != null) {
                        // Ground Identification Cache
                        p.movement.setDetectionLocation(false);
                        Player n = p.getPlayer();

                        if (n != null) {
                            // Only
                            p.ping = Latency.ping(n);
                            p.setFrozen(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && n.isFrozen());
                            p.setInvulnerable(p.isFrozen() || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && n.isInvulnerable());

                            // Bad
                            p.setHealth(n.getHealth());
                            p.setSleeping(n.isSleeping());
                            p.setInventory(n.getInventory(), n.getOpenInventory());

                            // Good
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                                ElytraUse.judge(p, n.isGliding(), false);
                            }
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                                p.movement.setSwimming(n.isSwimming(), -1);
                            }
                            p.setGameMode(n.getGameMode());
                            p.movement.setFlying(n.isFlying());
                            p.setFoodLevel(n.getFoodLevel(), false);
                            p.setDead(n.isDead());
                            p.maximumNoDamageTicks = n.getMaximumNoDamageTicks();

                            // Check Allowance Cache
                            if (TPS.getTick(p) <= 100
                                    || !MaximumCheckedPlayers.isChecked(p.uuid)
                                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getGameMode() == GameMode.SPECTATOR
                                    || p.getCancellableCompatibility() != null) {
                                p.runChecks[0] = false;
                                p.runChecks[1] = false;
                            } else {
                                p.runChecks[0] = !TPS.areLow(p);
                                p.runChecks[1] = true;
                            }

                            // External
                            p.playerProfile.playerCombat.track();
                            if (MultiVersion.folia || !SpartanBukkit.isSynchronised()) {
                                PlayerUtils.update(p, n, true);
                            }
                        } else {
                            p.runChecks[0] = false;
                            p.runChecks[1] = false;
                        }
                    }
                });
            }
        }
    }

    // Object

    public SpartanPlayer(Player p, UUID uuid) {
        PlayerInventory inv = p.getInventory();
        InventoryView openInventory = p.getOpenInventory();
        SpartanOpenInventory spartanOpenInv = new SpartanOpenInventory(
                openInventory.getCursor(),
                openInventory.countSlots(),
                openInventory.getTopInventory().getContents(),
                openInventory.getBottomInventory().getContents());
        Enums.HackType[] hackTypes = Enums.HackType.values();
        ItemStack itemInHand = p.getItemInHand();

        this.uuid = uuid;
        this.nearbyEntities = Collections.synchronizedList(new ArrayList<>());
        this.nearbyEntitiesDistance = 0;
        this.runChecks = new boolean[]{false, false};
        this.runCheck = new LinkedHashMap<>(hackTypes.length);
        this.runCheckAccountLag = new LinkedHashMap<>(hackTypes.length);
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? Enums.DataType.Bedrock : Enums.DataType.Java;
        this.handlers = new Handlers(this);

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.dead = p.isDead();
        this.sleeping = p.isSleeping();
        this.walkSpeed = p.getWalkSpeed();
        this.eyeHeight = p.getEyeHeight();
        this.flySpeed = p.getFlySpeed();
        this.gameMode = p.getGameMode();
        this.health = p.getHealth();
        this.usingItem = p.isBlocking();
        this.openInventory = spartanOpenInv;
        this.maximumNoDamageTicks = p.getMaximumNoDamageTicks();
        this.fallDistance = p.getFallDistance();
        this.foodLevel = p.getFoodLevel();
        this.potionEffects = Collections.synchronizedList(new ArrayList<>(p.getActivePotionEffects()));
        this.ping = Latency.ping(p);
        this.frozen = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && p.isFrozen();
        this.lastPlayed = p.getLastPlayed();
        this.playerProfile = ResearchEngine.getPlayerProfile(this);
        this.clicks = new Clicks();
        this.vehicle = p.getVehicle();
        this.movement = new SpartanPlayerMovement(this, p);
        this.damageReceived = Collections.synchronizedMap(new LinkedHashMap<>());
        this.damageDealt = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastDamageReceived = new SpartanPlayerDamage(
                this,
                new EntityDamageEvent(p, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.0),
                this.movement.getLocation(),
                0L
        );
        this.lastDamageDealt = this.lastDamageReceived;

        // Load them all to keep their order
        for (EntityDamageEvent.DamageCause damageCause : EntityDamageEvent.DamageCause.values()) {
            this.damageReceived.put(
                    damageCause,
                    this.lastDamageReceived
            );
            this.damageDealt.put(
                    damageCause,
                    this.lastDamageReceived
            );
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            this.invulnerable = p.isInvulnerable();
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, inv.getItemInOffHand());
        } else {
            this.invulnerable = false;
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, null);
        }
        this.creationTime = System.currentTimeMillis();

        this.buffer = new Buffer[hackTypes.length + 1];
        this.timer = new Timer[hackTypes.length + 1];
        this.decimals = new Decimals[hackTypes.length + 1];
        this.tracker = new Tracker[hackTypes.length + 1];
        this.cooldowns = new Cooldowns[hackTypes.length + 1];
        this.executors = new CheckExecutor[hackTypes.length];
        this.violations = new LiveViolation[hackTypes.length];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            this.buffer[id] = new Buffer(this);
            this.timer[id] = new Timer();
            this.decimals[id] = new Decimals();
            this.tracker[id] = new Tracker();
            this.cooldowns[id] = new Cooldowns(this);
            this.violations[id] = new LiveViolation(this, hackType);

            switch (hackType) {
                case KillAura:
                    this.executors[id] = new KillAura(this);
                    break;
                case Exploits:
                    this.executors[id] = new Exploits(this);
                    break;
                case HitReach:
                    this.executors[id] = new HitReach(this);
                    break;
                case Velocity:
                    this.executors[id] = new VelocityCheck(this);
                    break;
                case Speed:
                    this.executors[id] = new Speed(this);
                    break;
                case NoSwing:
                    this.executors[id] = new NoSwing(this);
                    break;
                case IrregularMovements:
                    this.executors[id] = new IrregularMovements(this);
                    break;
                case NoFall:
                    this.executors[id] = new NoFall(this);
                    break;
                case GhostHand:
                    this.executors[id] = new GhostHand(this);
                    break;
                case BlockReach:
                    this.executors[id] = new BlockReach(this);
                    break;
                case FastBreak:
                    this.executors[id] = new FastBreak(this);
                    break;
                case FastClicks:
                    this.executors[id] = new FastClicks(this);
                    break;
                case Criticals:
                    this.executors[id] = new Criticals(this);
                    break;
                case MorePackets:
                    this.executors[id] = new MorePackets(this);
                    break;
                case ImpossibleActions:
                    this.executors[id] = new ImpossibleActions(this);
                    break;
                case FastPlace:
                    this.executors[id] = new FastPlace(this);
                    break;
                case NoSlowdown:
                    this.executors[id] = new NoSlowdown(this);
                    break;
                case AutoRespawn:
                    this.executors[id] = new AutoRespawn(this);
                    break;
                case FastBow:
                    this.executors[id] = new FastBow(this);
                    break;
                case FastEat:
                    this.executors[id] = new FastEat(this);
                    break;
                case FastHeal:
                    this.executors[id] = new FastHeal(this);
                    break;
                case ItemDrops:
                    this.executors[id] = new ItemDrops(this);
                    break;
                case InventoryClicks:
                    this.executors[id] = new InventoryClicks(this);
                    break;
                case ImpossibleInventory:
                    this.executors[id] = new ImpossibleInventory(this);
                    break;
                case XRay:
                    this.executors[id] = new XRay(this);
                    break;
                default:
                    break;
            }
        }
        this.lastViolation = this.violations[0];
        this.buffer[hackTypes.length] = new Buffer(this);
        this.timer[hackTypes.length] = new Timer();
        this.decimals[hackTypes.length] = new Decimals();
        this.tracker[hackTypes.length] = new Tracker();
        this.cooldowns[hackTypes.length] = new Cooldowns(this);
    }

    public boolean debug(boolean broadcast, boolean cutDecimals, Object... message) {
        if (SpartanBukkit.testMode) {
            Player p = getPlayer();

            if (p != null && p.isWhitelisted()) {
                String string = "§f" + p.getName() + " ";

                if (message == null || message.length == 0) {
                    string += new Random().nextInt();
                } else {
                    int i = 0;

                    if (cutDecimals) {
                        for (Object object : message) {
                            if (object instanceof Double) {
                                message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7")
                                        + AlgebraUtils.cut((double) object, GroundUtils.maxHeightLength);
                            } else {
                                message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7") + object;
                            }
                            i++;
                        }
                    } else {
                        for (Object object : message) {
                            message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7") + object;
                            i++;
                        }
                    }
                    string += StringUtils.toString(message, " ");
                }
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    if (broadcast) {
                        Bukkit.broadcastMessage(string);
                    } else {
                        for (SpartanPlayer o : players) {
                            if (o != null && o.isOp()) {
                                Player no = o.getPlayer();

                                if (no != null) {
                                    no.sendMessage(string);
                                }
                            }
                        }
                    }
                }
                Bukkit.getConsoleSender().sendMessage(string);
                return true;
            }
        }
        return false;
    }

    // Separator

    public Buffer getBuffer() {
        return buffer[Enums.HackType.values().length];
    }

    public Timer getTimer() {
        return timer[Enums.HackType.values().length];
    }

    public Decimals getDecimals() {
        return decimals[Enums.HackType.values().length];
    }

    public Tracker getTracker() {
        return tracker[Enums.HackType.values().length];
    }

    public Cooldowns getCooldowns() {
        return cooldowns[Enums.HackType.values().length];
    }

    public Buffer getBuffer(Enums.HackType hackType) {
        return buffer[hackType.ordinal()];
    }

    public Timer getTimer(Enums.HackType hackType) {
        return timer[hackType.ordinal()];
    }

    public Decimals getDecimals(Enums.HackType hackType) {
        return decimals[hackType.ordinal()];
    }

    public Tracker getTracker(Enums.HackType hackType) {
        return tracker[hackType.ordinal()];
    }

    public Cooldowns getCooldowns(Enums.HackType hackType) {
        return cooldowns[hackType.ordinal()];
    }

    public CheckExecutor getExecutor(Enums.HackType hackType) {
        return executors[hackType.ordinal()];
    }

    // Separator

    public Handlers getHandlers() {
        return handlers;
    }

    public void resetHandlers() {
        for (Handlers.HandlerType handlerType : Handlers.HandlerType.values()) {
            handlers.remove(handlerType);
        }
    }

    // Separator

    public LiveViolation[] getViolations() {
        return violations;
    }

    public LiveViolation getViolations(Enums.HackType hackType) {
        return violations[hackType.ordinal()];
    }

    public LiveViolation getLastViolation() {
        return lastViolation;
    }

    public void setLastViolation(LiveViolation liveViolation) {
        this.lastViolation = liveViolation;
    }

    // Separator

    public Clicks getClicks() {
        return clicks;
    }

    public void calculateClicks(Action action, boolean runRegardless) {
        if (!runRegardless) {
            if (playerProfile.playerCombat.isActivelyFighting(null, true, true, false)) {
                runRegardless = true;
            } else {
                SpartanBlock block = getTargetBlock(CombatUtils.maxHitDistance);
                runRegardless = block == null || BlockUtils.areAir(block.material);
            }
        }

        if (runRegardless
                && (action == null || action == Action.LEFT_CLICK_AIR)) {
            clicks.calculate();

            if (clicks.canDistributeInformation()) {
                InteractiveInventory.playerInfo.refresh(name);
            }
        }
    }

    // Separator

    public int getMaxChatLength() {
        return bedrockPlayer ? 512 :
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) || PluginUtils.exists("viaversion") || PluginUtils.exists("protocolsupport") ? 256 :
                        100;
    }

    public void sendMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            p.sendMessage(message);
        }
    }

    public void sendInventoryCloseMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            if (message != null) {
                p.sendMessage(message);
            }
            p.closeInventory();
        }
    }

    public void sendImportantMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
            p.closeInventory();
        }
    }

    // Separator

    public void openInventory(Inventory inventory) {
        if (inventory != null) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.openInventory(inventory);
            }
        }
    }

    public Inventory createInventory(int size, String title) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            return Bukkit.createInventory(p, size, title);
        }
        return null;
    }

    public SpartanOpenInventory getOpenInventory() {
        return openInventory;
    }

    // Separator

    public SpartanInventory getInventory() {
        return inventory;
    }

    public ItemStack getItemInHand() {
        return inventory.itemInHand;
    }

    public synchronized void setInventory(PlayerInventory inv, InventoryView openInventory) {
        this.inventory = new SpartanInventory(inv.getContents(),
                new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()},
                inv.getItemInHand(), MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? inv.getItemInOffHand() : null);

        if (openInventory != null) {
            this.openInventory = new SpartanOpenInventory(openInventory.getCursor(),
                    openInventory.countSlots(),
                    openInventory.getTopInventory().getContents(),
                    openInventory.getBottomInventory().getContents());
        } else {
            this.openInventory = new SpartanOpenInventory();
        }
    }

    // Separator

    // Separator

    private boolean canRunChecks(boolean accountLag) {
        return runChecks[accountLag ? 0 : 1];
    }

    public boolean canRunCheck(Enums.HackType hackType, boolean accountLag) {
        if (canRunChecks(accountLag)) {
            Map<Enums.HackType, Boolean> map = accountLag ? runCheckAccountLag : runCheck;
            Boolean bool = map.get(hackType);

            if (bool != null) {
                return bool;
            }
            bool = SpartanEdition.supportsCheck(dataType, hackType)
                    && hackType.getCheck().isEnabled(dataType, this.getWorld().getName(), this)
                    && !Permissions.isBypassing(this, hackType);
            map.put(hackType, bool);
            return bool;
        }
        return false;
    }

    public boolean isOnFire() {
        return !this.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)
                && (this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE).ticksPassed() <= 5
                || this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE_TICK).ticksPassed() <= 5);
    }

    // Separator

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOp() {
        Player p = this.getPlayer();
        return p != null && p.isOp();
    }

    public boolean isWhitelisted() {
        Player p = this.getPlayer();
        return p != null && p.isWhitelisted();
    }

    // Separator

    public boolean isDead() {
        return dead;
    }

    public synchronized void setDead(boolean dead) {
        this.dead = dead;
    }

    // Separator

    public boolean isSleeping() {
        return sleeping;
    }

    public synchronized void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    // Separator

    public boolean isOnGroundCustom() {
        return onGroundCustom;
    }

    public boolean isOnGround() {
        Entity vehicle = getVehicle();

        if (vehicle != null) {
            return vehicle.isOnGround();
        } else {
            Player p = this.getPlayer();
            return p != null && p.isOnGround();
        }
    }

    public int getBlocksOffGround(int limit) {
        SpartanLocation ploc = movement.getLocation();
        Decimals decimals = getDecimals();
        Cooldowns cooldowns = getCooldowns();
        String key = "blocks-off-ground=" + Objects.hash(ploc.getBlockY(), limit);

        if (!cooldowns.canDo(key)) {
            return (int) decimals.get(key, 0.0);
        }
        int blocksOffGround = 0;

        for (double i = 0.0; i <= ploc.getY(); i++) {
            if (!GroundUtils.isOnGround(this, ploc.clone().add(0, -i, 0), -i, true, true)) {
                blocksOffGround++;

                if (blocksOffGround == limit) {
                    break;
                }
            } else {
                break;
            }
        }
        decimals.set(key, blocksOffGround);
        cooldowns.add(key, 4);
        return blocksOffGround;
    }

    public boolean isGroundSpoofing() {
        return isOnGroundCustom() != isOnGround();
    }

    public boolean refreshOnGroundCustom(SpartanLocation from) {
        if (GroundUtils.isOnGround(this, this.movement.getLocation(), 0.0, true, true)) {
            this.onGroundCustom = true; // Server Current Location
        } else if (from != null && GroundUtils.isOnGround(this, from, 0.0, true, true)) {
            this.onGroundCustom = true; // Server Previous Location
        } else {
            SpartanLocation eventFrom = this.movement.getEventFromLocation();

            Runnable runnable = () -> {
                if (GroundUtils.isOnGround(this, eventFrom, 0.0, true, true)) {
                    this.onGroundCustom = true; // Event Previous Location
                } else {
                    SpartanLocation eventLocation = this.movement.getEventToLocation();
                    this.onGroundCustom = eventLocation != null
                            && GroundUtils.isOnGround(this, eventLocation, 0.0, true, true); // Event Current Location
                }
            };

            if (MultiVersion.folia) {
                SpartanBukkit.runTask(
                        eventFrom.world,
                        eventFrom.getBlockX() >> 4,
                        eventFrom.getBlockZ() >> 4,
                        runnable
                );
            } else {
                runnable.run();
            }
        }
        return this.onGroundCustom;
    }

    // Separator

    public void kick(CommandSender punisher, String reason) {
        Player target = getPlayer();

        if (target != null && target.isOnline()) {
            String punisherName = punisher instanceof ConsoleCommandSender
                    ? Config.messages.getColorfulString("console_name")
                    : punisher.getName(),
                    kick = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("kick_reason")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null),
                    announcement = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("kick_broadcast_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null);

            if (Config.settings.getBoolean("Punishments.broadcast_on_punishment")) {
                Bukkit.broadcastMessage(announcement);
            } else {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    for (SpartanPlayer o : players) {
                        if (DetectionNotifications.hasPermission(o)) {
                            o.sendMessage(announcement);
                        }
                    }
                }
            }

            playerProfile.punishmentHistory.increaseKicks(this, kick);
            target.kickPlayer(kick);
        }
    }

    public void warn(CommandSender punisher, String reason) {
        Player target = getPlayer();

        if (target != null && target.isOnline()) {
            String punisherName = punisher instanceof ConsoleCommandSender
                    ? Config.messages.getColorfulString("console_name")
                    : punisher.getName(),
                    warning = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("warning_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null),
                    feedback = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("warning_feedback_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null);
            target.sendMessage(warning);
            punisher.sendMessage(feedback);

            playerProfile.punishmentHistory.increaseWarnings(this, reason);
        }
    }

    public PlayerProfile getProfile() {
        return playerProfile;
    }

    public synchronized void setProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    // Separator

    public SpartanBlock getTargetBlock(double distance) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            try {
                Player n = getPlayer();

                if (n != null) {
                    Block block = n.getTargetBlock(null, (int) distance);

                    if (block != null) {
                        return new SpartanBlock(this, block);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public SpartanBlock getIllegalTargetBlock(SpartanBlock clickedBlock) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            SpartanLocation clickedBlockLocation = clickedBlock.getLocation();
            boolean editableClickedBlock = BlockUtils.isChangeable(clickedBlockLocation);

            if (!editableClickedBlock && !BlockUtils.isSolid(clickedBlockLocation)) {
                return null;
            }
            double distance = movement.getLocation().distance(clickedBlockLocation);

            if (distance >= 0.5) {
                try {
                    SpartanBlock targetBlock = getTargetBlock(Math.max(Math.floor(distance), 1.0));

                    if (targetBlock != null && BlockUtils.isSolid(targetBlock.material)) {
                        SpartanLocation targetBlockLocation = targetBlock.getLocation();

                        if (clickedBlockLocation.distance(targetBlockLocation) >= (handlers.has(Handlers.HandlerType.ElytraUse) ? 2.5 : BlockUtils.areEggs(getItemInHand().getType()) ? 2.0 : 1.0)

                                && (targetBlock.x != clickedBlock.x
                                || targetBlock.y != clickedBlock.y
                                || targetBlock.z != clickedBlock.z)

                                && (editableClickedBlock && !BlockUtils.isLiquid(targetBlockLocation)
                                || BlockUtils.isFullSolid(targetBlockLocation))) {
                            return targetBlock;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    // Separator

    public Entity getVehicle() {
        return vehicle;
    }

    public synchronized void setVehicle(Entity vehicle) {
        this.vehicle = vehicle;
    }

    // Separator

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public synchronized void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    // Separator

    public float getFlySpeed() {
        return flySpeed;
    }

    public synchronized void setFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    // Separator

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public synchronized void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    // Separator

    public int getFoodLevel() {
        return foodLevel;
    }

    public synchronized void setFoodLevel(int foodLevel, boolean modify) {
        if (modify) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.setFoodLevel(foodLevel);
                this.foodLevel = foodLevel;
            }
        } else {
            this.foodLevel = foodLevel;
        }
    }

    // Separator

    public double getHealth() {
        return health;
    }

    public synchronized void setHealth(double health) {
        this.health = health;
    }

    // Separator

    public float getFallDistance() {
        return fallDistance;
    }

    public synchronized void setFallDistance(float fallDistance, boolean modify) {
        if (modify) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.setFallDistance(fallDistance);
                this.fallDistance = fallDistance;
            }
        } else {
            this.fallDistance = fallDistance;
        }
    }

    // Separator

    public GameMode getGameMode() {
        return gameMode;
    }

    public synchronized void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    // Separator

    public double getEyeHeight() {
        return eyeHeight;
    }

    public synchronized void setEyeHeight(double eyeHeight) {
        this.eyeHeight = eyeHeight;
    }

    // Separator

    public boolean isFrozen() {
        return frozen;
    }

    public synchronized void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    // Separator

    public boolean isUsingItem() {
        return usingItem;
    }

    public synchronized void setUsingItem(boolean usingItem) {
        this.usingItem = usingItem;
    }

    public boolean hasShield() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && !Compatibility.CompatibilityType.OldCombatMechanics.isFunctional()
                && (isUsingItem()
                || getItemInHand().getType() == Material.SHIELD
                || inventory.itemInOffHand.getType() == Material.SHIELD);
    }

    // Separator

    public SpartanPlayerDamage getLastDamageReceived() {
        return this.lastDamageReceived;
    }

    public SpartanPlayerDamage getLastDamageDealt() {
        return this.lastDamageDealt;
    }

    public Set<Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage>> getRawReceivedDamages() {
        synchronized (this.damageReceived) {
            return new HashSet<>(this.damageReceived.entrySet());
        }
    }

    public Collection<SpartanPlayerDamage> getReceivedDamages() {
        synchronized (this.damageReceived) {
            return new ArrayList<>(this.damageReceived.values());
        }
    }

    public Set<Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage>> getRawDealtDamages() {
        synchronized (this.damageDealt) {
            return new HashSet<>(this.damageDealt.entrySet());
        }
    }

    public Collection<SpartanPlayerDamage> getDealtDamages() {
        synchronized (this.damageDealt) {
            return new ArrayList<>(this.damageDealt.values());
        }
    }

    public SpartanPlayerDamage getDamageReceived(EntityDamageEvent.DamageCause cause) {
        synchronized (this.damageReceived) {
            return this.damageReceived.get(cause);
        }
    }

    public SpartanPlayerDamage getDamageDealt(EntityDamageEvent.DamageCause cause) {
        synchronized (this.damageDealt) {
            return this.damageDealt.get(cause);
        }
    }

    public void addReceivedDamage(EntityDamageEvent event) {
        this.lastDamageReceived = new SpartanPlayerDamage(this, event, this.movement.getLocation());

        synchronized (this.damageReceived) {
            this.damageReceived.put(
                    event.getCause(),
                    this.lastDamageReceived
            );
        }
    }

    public void addDealtDamage(EntityDamageByEntityEvent event) {
        this.lastDamageDealt = new SpartanPlayerDamage(this, event, this.movement.getLocation());

        synchronized (this.damageDealt) {
            this.damageDealt.put(
                    event.getCause(),
                    this.lastDamageDealt
            );
        }
    }

    public int getMaximumNoDamageTicks() {
        return maximumNoDamageTicks;
    }

    // Separator

    public boolean hasAttackCooldown() {
        return getAttackCooldown() != 1.0f;
    }

    public float getAttackCooldown() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            Player player = getPlayer();
            return player == null ? 1.0f : player.getAttackCooldown();
        }
        return 1.0f;
    }

    // Separator

    private Collection<PotionEffect> getLocalActivePotionEffects() {
        Entity vehicle = getVehicle();
        return new ArrayList<>(
                vehicle != null ?
                        (vehicle instanceof LivingEntity ? ((LivingEntity) vehicle).getActivePotionEffects() : new ArrayList<>(0))
                        : potionEffects
        );
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        synchronized (potionEffects) {
            return getLocalActivePotionEffects();
        }
    }

    public void setActivePotionEffects(Collection<PotionEffect> effects) {
        synchronized (potionEffects) {
            this.potionEffects.clear();
            this.potionEffects.addAll(effects);
        }
    }

    public void removePotionEffect(PotionEffectType potionEffectType) {
        if (hasPotionEffect(potionEffectType)) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.removePotionEffect(potionEffectType);
                setActivePotionEffects(p.getActivePotionEffects());
            }
        }
    }

    public PotionEffect getPotionEffect(PotionEffectType type) {
        synchronized (potionEffects) {
            Collection<PotionEffect> potionEffects = getLocalActivePotionEffects();

            if (!potionEffects.isEmpty()) {
                for (PotionEffect potionEffect : potionEffects) {
                    if (potionEffect != null && potionEffect.getType().equals(type)) {
                        return potionEffect;
                    }
                }
            }
        }
        return null;
    }

    public boolean hasPotionEffect(PotionEffectType[] potionEffectTypes) {
        synchronized (potionEffects) {
            Collection<PotionEffect> potionEffects = getLocalActivePotionEffects();

            if (!potionEffects.isEmpty()) {
                for (PotionEffect potionEffect : potionEffects) {
                    for (PotionEffectType potionEffectType : potionEffectTypes) {
                        if (potionEffect.getType().equals(potionEffectType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean hasPotionEffect(PotionEffectType potionEffectType) {
        synchronized (potionEffects) {
            Collection<PotionEffect> potionEffects = getLocalActivePotionEffects();

            if (!potionEffects.isEmpty()) {
                for (PotionEffect potionEffect : potionEffects) {
                    if (potionEffect.getType().equals(potionEffectType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Separator

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        double max = Math.max(Math.max(x, y), z);

        synchronized (nearbyEntities) {
            if (max > nearbyEntitiesDistance) {
                Player p = this.getPlayer();

                if (p != null) {
                    nearbyEntitiesDistance = max;

                    if (MultiVersion.folia) {
                        SpartanBukkit.runTask(this, () -> {
                            nearbyEntities.clear();
                            nearbyEntities.addAll(p.getNearbyEntities(x, y, z));
                        });
                        return new ArrayList<>(nearbyEntities);
                    } else if (SpartanBukkit.isSynchronised()) {
                        List<Entity> entities = p.getNearbyEntities(x, y, z);
                        nearbyEntities.clear();
                        nearbyEntities.addAll(entities);
                        return entities;
                    } else {
                        SpartanLocation loc = this.movement.getLocation();
                        List<Entity> entities;

                        synchronized (worldEntities) {
                            entities = worldEntities.get(loc.world);
                        }
                        if (entities != null && !entities.isEmpty()) {
                            entities = new ArrayList<>(entities);
                            Entity vehicle = this.getVehicle();

                            if (vehicle != null) {
                                entities.remove(vehicle);
                            }
                            entities.remove(p);
                            Iterator<Entity> iterator = entities.iterator();

                            while (iterator.hasNext()) {
                                Entity entity = iterator.next();

                                if ((entity instanceof LivingEntity ? entity.isDead() : !(entity instanceof Vehicle))
                                        || !loc.isNearbyChunk(entity.getLocation())
                                        || loc.distance(entity.getLocation()) > max) {
                                    iterator.remove();
                                }
                            }
                            nearbyEntities.clear();
                            nearbyEntities.addAll(entities);
                            return entities;
                        } else {
                            nearbyEntities.clear();
                            return new ArrayList<>(0);
                        }
                    }
                } else {
                    nearbyEntities.clear();
                    return new ArrayList<>(0);
                }
            } else if (!nearbyEntities.isEmpty()) {
                SpartanLocation loc = this.movement.getLocation();
                List<Entity> entities = new ArrayList<>(nearbyEntities);
                Entity vehicle = this.getVehicle();

                if (vehicle != null) {
                    entities.remove(vehicle);
                }
                Iterator<Entity> iterator = entities.iterator();

                while (iterator.hasNext()) {
                    Entity entity = iterator.next();

                    if (entity instanceof LivingEntity && entity.isDead()
                            || loc.distance(entity.getLocation()) > max) {
                        iterator.remove();
                    }
                }
                nearbyEntities.clear();
                nearbyEntities.addAll(entities);
                return entities;
            } else {
                return new ArrayList<>(0);
            }
        }
    }

    public int getEnemiesNumber(double distance, boolean includeRest) {
        Player n = getPlayer();

        if (n != null) {
            List<Entity> entities = getNearbyEntities(distance, distance, distance);

            if (!entities.isEmpty()) {
                int count = 0;

                for (Entity e : getNearbyEntities(distance, distance, distance)) {
                    if (e instanceof Monster) {
                        LivingEntity target = ((Monster) e).getTarget();

                        if (target != null && target.equals(n)) {
                            count++;
                        }
                    } else if (e instanceof Player) {
                        SpartanPlayer target = SpartanBukkit.getPlayer((Player) e);

                        if (target != null
                                && playerProfile.playerCombat.isActivelyFighting(
                                target,
                                true,
                                true,
                                false)
                        ) {
                            count++;
                        }
                    } else if (includeRest && e instanceof LivingEntity) {
                        count++;
                    }
                }
                return count;
            }
        }
        return 0;
    }

    // Teleport

    public boolean safeTeleport(SpartanLocation loc) {
        if (this.getWorld().equals(loc.world)) {
            return this.teleport(loc);
        } else {
            return false;
        }
    }

    public boolean groundTeleport(boolean checkGround) {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection")) {
            return false;
        }
        SpartanLocation location = this.movement.getLocation();

        if (checkGround
                && this.onGroundCustom
                && GroundUtils.isOnGround(this, location, 0.0, false, false)) {
            return false;
        }
        SpartanLocation locationP1 = location.clone().add(0, 1, 0);

        if (BlockUtils.isSolid(locationP1)
                && !(BlockUtils.areWalls(locationP1) || BlockUtils.canClimb(locationP1))) {
            return false;
        }
        World world = getWorld();
        float countedDamage = 0.0f;
        double startY = Math.min(BlockUtils.getMaxHeight(world), location.getY()),
                box = startY - Math.floor(startY);
        int iterations = 0;

        if (!GroundUtils.heightExists(box)) {
            box = 0.0;
        }

        for (double progressiveY = startY; startY > BlockUtils.getMinHeight(world); progressiveY--) {
            SpartanLocation loopLocation = location.clone().add(0.0, -(startY - progressiveY), 0.0);
            Material material = loopLocation.getBlock().material;

            if (iterations != PlayerUtils.chunk
                    && (BlockUtils.canClimb(material)
                    || BlockUtils.areWalls(material)
                    || !BlockUtils.isSolid(material))) {
                countedDamage++;
                iterations++;
            } else {
                double blockBox = GroundUtils.getMaxHeight(material);

                if (blockBox == 0.0 || blockBox == 0.5) {
                    loopLocation.setY(Math.floor(progressiveY) + 1.0);
                } else {
                    loopLocation.setY(Math.floor(progressiveY) + Math.max(blockBox, box));
                }
                safeTeleport(loopLocation);
                break;
            }
        }
        countedDamage--; // Ignoring the first check of the current location

        if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")) { // Damage
            if (countedDamage >= Math.max(fallDistance, 4.0)) { // Greater than fall
                setFallDistance(0.0f, true);

                if (playerProfile.isSuspectedOrHacker()) {
                    applyFallDamage(countedDamage);
                }
            } else if (countedDamage > 0.0f) {
                setFallDistance(0.0f, true);
            }
        } else if (countedDamage > 0.0f) {
            setFallDistance(0.0f, true);
        }
        return true;
    }

    // Separator

    public boolean teleport(SpartanLocation location) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.leaveVehicle();
            this.movement.setSprinting(false);
            this.movement.setSneaking(false);
            this.movement.setSwimming(false, 0);
            this.movement.setGliding(false, false);
            this.movement.removeLastLiquidTime();

            if (MultiVersion.folia) {
                p.teleportAsync(location.getBukkitLocation());
            } else {
                p.teleport(location.getBukkitLocation());
            }
            return true;
        } else {
            return false;
        }
    }

    // Separator

    public boolean applyFallDamage(double d) {
        handlers.disable(Handlers.HandlerType.Velocity, 3);
        return this.damage(d, EntityDamageEvent.DamageCause.FALL);
    }

    public boolean damage(double amount, EntityDamageEvent.DamageCause damageCause) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            EntityDamageEvent event = new EntityDamageEvent(p, damageCause, amount);
            p.damage(amount);
            p.setLastDamageCause(event);
            this.addReceivedDamage(event);
            return true;
        } else {
            return false;
        }
    }

    // Separator

    public boolean canDo(boolean bypassItemAttributes) {
        if ((bypassItemAttributes || !Attributes.has(this, Attributes.GENERIC_MOVEMENT_SPEED))
                && !isDead() && !isSleeping() && canRunChecks(true) && !this.movement.wasFlying() && !Velocity.hasCooldown(this)) {
            if (Compatibility.CompatibilityType.MythicMobs.isFunctional() || Compatibility.CompatibilityType.ItemsAdder.isFunctional()) {
                List<Entity> entities = getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (!entities.isEmpty()) {
                    for (Entity entity : entities) {
                        if (MythicMobs.is(entity) || ItemsAdder.is(entity)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public String getCancellableCompatibility() {
        return MythicMobs.is(this) ? Compatibility.CompatibilityType.MythicMobs.toString() :
                ItemsAdder.is(this) ? Compatibility.CompatibilityType.ItemsAdder.toString() :
                        CustomEnchantsPlus.has(this) ? Compatibility.CompatibilityType.CustomEnchantsPlus.toString() :
                                EcoEnchants.has(this) ? Compatibility.CompatibilityType.EcoEnchants.toString() : null;
    }

    // Separator

    public World getWorld() {
        return movement.getLocation().world;
    }

    public boolean isOutsideOfTheBorder(double deviation) {
        SpartanLocation loc = movement.getLocation();
        WorldBorder border = getWorld().getWorldBorder();
        double size = (border.getSize() / 2.0) + deviation;
        Location center = border.getCenter();
        return Math.abs(loc.getX() - center.getX()) > size
                || Math.abs(loc.getZ() - center.getZ()) > size;
    }

    // Separator

    public boolean canSee(Player target) {
        if (SpartanBukkit.supportedFork) {
            Player player = getPlayer();

            if (player != null) {
                return player.canSee(target);
            }
        }
        return true;
    }

    public int getPing() {
        return ping;
    }
}
