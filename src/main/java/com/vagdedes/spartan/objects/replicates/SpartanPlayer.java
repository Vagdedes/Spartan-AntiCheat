package com.vagdedes.spartan.objects.replicates;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.checks.combat.FastBow;
import com.vagdedes.spartan.checks.combat.HitReach;
import com.vagdedes.spartan.checks.combat.VelocityCheck;
import com.vagdedes.spartan.checks.combat.criticals.Criticals;
import com.vagdedes.spartan.checks.combat.fastClicks.FastClicks;
import com.vagdedes.spartan.checks.combat.killAura.KillAura;
import com.vagdedes.spartan.checks.exploits.Exploits;
import com.vagdedes.spartan.checks.inventory.ImpossibleInventory;
import com.vagdedes.spartan.checks.inventory.InventoryClicks;
import com.vagdedes.spartan.checks.inventory.ItemDrops;
import com.vagdedes.spartan.checks.movement.MorePackets;
import com.vagdedes.spartan.checks.movement.NoFall;
import com.vagdedes.spartan.checks.movement.NoSlowdown;
import com.vagdedes.spartan.checks.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.checks.movement.speed.Speed;
import com.vagdedes.spartan.checks.player.AutoRespawn;
import com.vagdedes.spartan.checks.player.FastEat;
import com.vagdedes.spartan.checks.player.FastHeal;
import com.vagdedes.spartan.checks.player.NoSwing;
import com.vagdedes.spartan.checks.world.*;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.compatibility.necessary.bedrock.BedrockCompatibility;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.protections.Latency;
import com.vagdedes.spartan.functionality.protections.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.connection.Piracy;
import com.vagdedes.spartan.handlers.identifiers.complex.predictable.Liquid;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.ElytraUse;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Velocity;
import com.vagdedes.spartan.handlers.stability.*;
import com.vagdedes.spartan.interfaces.listeners.EventsHandler7;
import com.vagdedes.spartan.objects.data.Timer;
import com.vagdedes.spartan.objects.data.*;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.*;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;

public class SpartanPlayer {

    private static final int tickFrequency = 5 * 1200;
    private static int syncTicks = 0;
    private static final Map<World, List<Entity>> worldEntities
            = Collections.synchronizedMap(new LinkedHashMap<>(Math.max(Bukkit.getWorlds().size(), 1)));

    private float walkSpeed, flySpeed, fallDistance;
    private boolean gliding, swimming, sprinting, sneaking, frozen, onGroundCustom,
            dead, sleeping, flying, usingItem, invulnerable;
    private int ping, maximumNoDamageTicks, foodLevel, fireTicks;
    private double eyeHeight, health;
    private GameMode gameMode;
    private final boolean bedrockPlayer;
    private long damageTick, lastHeadMovement;
    private final long creationTime, lastPlayed;
    private final String name;
    private final UUID uuid;
    private final String ipAddress;
    private final Collection<PotionEffect> potionEffects;
    private final ResearchEngine.DataType dataType;
    private final InetSocketAddress address;
    private EntityDamageEvent lastDamageCause;
    private SpartanInventory inventory;
    private SpartanOpenInventory openInventory;

    // Data

    private final Buffer[] buffer;
    private final Timer[] timer;
    private final Decimals[] decimals;
    private final Tracker[] tracker;
    private final Cooldowns[] cooldowns;
    private final CheckExecutor[] executors;
    private final LiveViolation[] violations;
    private final Handlers handlers;
    private LiveViolation lastViolation;

    // Cache

    private PlayerProfile playerProfile;
    private final Clicks clicks;

    private SpartanLocation location, fromLocation, eventTo, eventFrom;
    private long locationTime;

    private Entity vehicle;
    private final List<Entity> nearbyEntities;
    private double nearbyEntitiesDistance;

    private final boolean[] runChecks;
    private final Map<Enums.HackType, Boolean> runCheck;
    private final Map<Enums.HackType, Boolean> runCheckAccountLag;

    // Custom

    private final Map<Long, Double>
            nmsDistance, nmsHorizontalDistance, nmsVerticalDistance,
            nmsBox;
    private double
            customDistance, customHorizontalDistance, customVerticalDistance;
    private long
            lastOffSprint,
            lastFall,
            lastJump,
            swimmingTime,
            flyingTime,
            lastLiquidTime;
    private int
            airTicks, oldAirTicks,
            fallingTicks,
            groundTicks,
            extraPackets;

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

                if (syncTicks % 20 == 0) {
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
                        Cache.clearCheckCache(p);
                    }
                    p.nearbyEntitiesDistance = 0;
                    p.locationTime = 0L;
                    p.runCheck.clear();
                    p.runCheckAccountLag.clear();

                    // Separator
                    SpartanBukkit.runTask(p, () -> {
                        Player n = p.getPlayer();

                        if (n != null) {
                            Collection<PotionEffect> potionEffects = n.getActivePotionEffects();
                            p.setActivePotionEffects(potionEffects); // Bad
                            PlayerData.run(p, n, potionEffects);
                        }

                        // Buffer Cache
                        for (Buffer buffer : p.buffer) {
                            buffer.run(p);
                        }

                        // Preventions
                        for (Enums.HackType hackType : EventsHandler7.handledChecks) {
                            if (p.getViolations(hackType).process()) {
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
                        p.ping = -1;
                        Player n = p.getPlayer();

                        if (n != null) {
                            // Only
                            p.setFireTicks(n.getFireTicks());
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
                                p.setSwimming(n.isSwimming(), -1);
                            }
                            p.setGameMode(n.getGameMode());
                            p.setFlying(n.isFlying());
                            p.setFoodLevel(n.getFoodLevel(), false);
                            p.setDead(n.isDead());
                            EntityDamageEvent lastDamageCause = n.getLastDamageCause();

                            if (lastDamageCause != null) {
                                p.setLastDamageCause(lastDamageCause, n.getMaximumNoDamageTicks());
                            }

                            // Check Allowance Cache
                            if (TPS.getTick(p) <= 100
                                    || !MaximumCheckedPlayers.isChecked(p.getUniqueId())
                                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getGameMode() == GameMode.SPECTATOR
                                    || IDs.isPreview()
                                    || p.getCancellableCompatibility() != null) {
                                p.runChecks[0] = false;
                                p.runChecks[1] = false;
                            } else {
                                p.runChecks[0] = !TPS.areLow(p);
                                p.runChecks[1] = true;
                            }

                            // External
                            p.playerProfile.getCombat().track();
                            if (MultiVersion.folia || !SpartanBukkit.isSynchronised()) {
                                PlayerData.update(p, n, true);
                            }
                            DetectionLocation.run(p);
                        } else {
                            p.runChecks[0] = false;
                            p.runChecks[1] = false;
                        }
                    }
                });
            }
        }
    }

    // Separator

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
        this.location = new SpartanLocation(this, p.getLocation());
        this.fromLocation = this.location;
        this.eventTo = this.location;
        this.eventFrom = this.location;
        this.locationTime = 0L;
        this.nearbyEntities = Collections.synchronizedList(new ArrayList<>());
        this.nearbyEntitiesDistance = 0;
        this.runChecks = new boolean[]{false, false};
        this.runCheck = new LinkedHashMap<>(hackTypes.length);
        this.runCheckAccountLag = new LinkedHashMap<>(hackTypes.length);
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? ResearchEngine.DataType.Bedrock : ResearchEngine.DataType.Java;
        this.handlers = new Handlers(this);

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.flying = p.isFlying();
        this.dead = p.isDead();
        this.sleeping = p.isSleeping();
        this.walkSpeed = p.getWalkSpeed();
        this.sprinting = p.isSprinting();
        this.sneaking = p.isSneaking();
        this.eyeHeight = p.getEyeHeight();
        this.flySpeed = p.getFlySpeed();
        this.gameMode = p.getGameMode();
        this.health = p.getHealth();
        this.address = p.getAddress();
        this.usingItem = p.isBlocking();
        this.fireTicks = p.getFireTicks();
        this.damageTick = 0L;
        this.lastDamageCause = p.getLastDamageCause();
        this.openInventory = spartanOpenInv;
        this.maximumNoDamageTicks = p.getMaximumNoDamageTicks();
        this.fallDistance = p.getFallDistance();
        this.foodLevel = p.getFoodLevel();
        this.potionEffects = Collections.synchronizedList(new ArrayList<>(p.getActivePotionEffects()));
        this.ping = Latency.ping(p);
        this.frozen = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && p.isFrozen();
        this.lastPlayed = p.getLastPlayed();
        this.customDistance = 0.0;
        this.customHorizontalDistance = 0.0;
        this.customVerticalDistance = 0.0;
        this.nmsDistance = new LinkedHashMap<>();
        this.nmsHorizontalDistance = new LinkedHashMap<>();
        this.nmsVerticalDistance = new LinkedHashMap<>();
        this.nmsBox = new LinkedHashMap<>();
        this.lastFall = 0L;
        this.lastOffSprint = 0L;
        this.lastJump = 0L;
        this.airTicks = 0;
        this.oldAirTicks = 0;
        this.fallingTicks = 0;
        this.groundTicks = 0;
        this.extraPackets = 0;
        this.swimmingTime = 0L;
        this.flyingTime = 0L;
        this.lastLiquidTime = 0L;
        this.lastHeadMovement = System.currentTimeMillis();
        this.playerProfile = ResearchEngine.getPlayerProfile(this);
        this.clicks = new Clicks();
        this.vehicle = p.getVehicle();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            this.gliding = p.isGliding();
            this.invulnerable = p.isInvulnerable();
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, inv.getItemInOffHand());

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                this.swimming = p.isSwimming();
            } else {
                this.swimming = false;
            }
        } else {
            this.gliding = false;
            this.invulnerable = false;
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, null);
            this.swimming = false;
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
            this.buffer[id] = new Buffer(this, hackType);
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
        this.buffer[hackTypes.length] = new Buffer(this, null);
        this.timer[hackTypes.length] = new Timer();
        this.decimals[hackTypes.length] = new Decimals();
        this.tracker[hackTypes.length] = new Tracker();
        this.cooldowns[hackTypes.length] = new Cooldowns(this);
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

    public LiveViolation[] getViolations() {
        return violations;
    }

    public LiveViolation getViolations(Enums.HackType hackType) {
        return violations[hackType.ordinal()];
    }

    public boolean isDetected(boolean prevention) {
        if (lastViolation.isDetected(prevention)) {
            return true;
        } else {
            for (LiveViolation liveViolation : violations) {
                if (liveViolation.isDetected(prevention)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean wasDetected(boolean prevention) {
        if (lastViolation.wasDetected(prevention)) {
            return true;
        } else {
            for (LiveViolation liveViolation : violations) {
                if (liveViolation.wasDetected(prevention)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getViolationCount() {
        int sum = 0;

        for (LiveViolation liveViolation : violations) {
            sum += liveViolation.getLevel();
        }
        return sum;
    }

    public LiveViolation getLastViolation() {
        return lastViolation;
    }

    public void setLastViolation(LiveViolation liveViolation) {
        this.lastViolation = liveViolation;
    }

    public Handlers getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public int getCPS() {
        return clicks.getCount();
    }

    public Clicks getClicks() {
        return clicks;
    }

    public void calculateClickData(Action action, boolean runRegardless) {
        if (!runRegardless) {
            if (playerProfile.getCombat().isActivelyFighting(null, true, true, false)) {
                runRegardless = true;
            } else {
                SpartanBlock block = getTargetBlock(CombatUtils.maxHitDistance);
                runRegardless = block == null || BlockUtils.areAir(block.getType());
            }
        }

        if (runRegardless) {
            boolean success;

            if (action == Action.LEFT_CLICK_AIR) {
                success = true;
                clicks.calculate();
            } else if (action == null) {
                if (clicks.getLastCalculation() > 50L) {
                    success = true;
                    clicks.calculate();
                } else {
                    success = false;
                }
            } else {
                success = false;
            }

            if (success && clicks.canDistributeInformation()) {
                SpartanMenu.playerInfo.refresh(getName());
            }
        }
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public ResearchEngine.DataType getDataType() {
        return dataType;
    }

    private SpartanLocation getVehicleLocation() {
        Entity vehicle = getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location vehicleLocation = vehicle.getLocation();
            SpartanLocation playerLocation = this.location;
            boolean isNull = playerLocation == null;
            return new SpartanLocation(this,
                    vehicleLocation,
                    isNull ? vehicleLocation.getYaw() : playerLocation.getYaw(),
                    isNull ? vehicleLocation.getPitch() : playerLocation.getPitch());
        }
        return null;
    }

    public void resetLocationData() {
        this.locationTime = 0L;
    }

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

    public SpartanLocation getLocation() {
        SpartanLocation vehicle = getVehicleLocation();

        if (vehicle != null) {
            return vehicle;
        } else {
            if (SpartanBukkit.isSynchronised()) {
                long ms = System.currentTimeMillis();

                if ((ms - this.locationTime) > TPS.tickTime) {
                    this.locationTime = ms;
                    Player p = getPlayer();

                    if (p != null) {
                        SpartanLocation from = this.location.clone();
                        this.location = new SpartanLocation(this, p.getLocation());

                        if (from != null) {
                            this.location.retrieveDataFrom(from);
                        }
                    } else {
                        this.location = new SpartanLocation();
                    }
                }
            }
            return location;
        }
    }

    public SpartanLocation getFromLocation() {
        return fromLocation;
    }

    public SpartanLocation getEventToLocation() {
        return eventTo;
    }

    public SpartanLocation getEventFromLocation() {
        return eventFrom;
    }

    public SpartanLocation setEventLocation(Location to) {
        SpartanLocation vehicle = getVehicleLocation();
        return vehicle != null ? vehicle : new SpartanLocation(this, to);
    }

    public boolean debug(boolean broadcast, boolean cutDecimals, Object... message) {
        if (!Piracy.enabled && !IDs.isBuiltByBit() && !IDs.isPolymart() && !IDs.isSongoda()
                && Bukkit.getMotd().contains(Register.plugin.getName())) {
            Player p = getPlayer();

            if (p != null && p.isWhitelisted()) {
                String string = ChatColor.GRAY + p.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.RED;

                if (message == null || message.length == 0) {
                    string += new Random().nextInt();
                } else {
                    if (cutDecimals) {
                        int i = 0;

                        for (Object object : message) {
                            if (object instanceof Double) {
                                message[i] = AlgebraUtils.cut((double) object, 5);
                            }
                            i++;
                        }
                    }
                    string += Arrays.toString(message);
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

    public boolean canRunChecks(boolean accountLag) {
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

    public int getFireTicks() {
        return fireTicks;
    }

    public boolean isOnFire() {
        return getFireTicks() > -20 && !hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    public synchronized void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
    }

    public boolean isOp() {
        Player p = this.getPlayer();
        return p != null && p.isOp();
    }

    public boolean isWhitelisted() {
        Player p = this.getPlayer();
        return p != null && p.isWhitelisted();
    }

    public void processLastMoveEvent(SpartanLocation to, SpartanLocation from) {
        this.eventTo = to;
        this.eventFrom = from;
    }

    public void setFromLocation(SpartanLocation loc) {
        this.fromLocation = loc;
    }

    public String getCancellableCompatibility() {
        return MythicMobs.is(this) ? Compatibility.CompatibilityType.MythicMobs.toString() :
                ItemsAdder.is(this) ? Compatibility.CompatibilityType.ItemsAdder.toString() :
                        CustomEnchantsPlus.has(this) ? Compatibility.CompatibilityType.CustomEnchantsPlus.toString() :
                                EcoEnchants.has(this) ? Compatibility.CompatibilityType.EcoEnchants.toString() : null;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isFlying() {
        Entity vehicle = getVehicle();
        return vehicle != null ? vehicle instanceof Player && ((Player) vehicle).isFlying() : flying;
    }

    public boolean wasFlying() {
        return isFlying() || flyingTime >= System.currentTimeMillis();
    }

    public boolean wasInLiquids() {
        return System.currentTimeMillis() - lastLiquidTime <= 755L;
    }

    public synchronized void setLastLiquidTime() {
        lastLiquidTime = System.currentTimeMillis();
    }

    public synchronized void removeLastLiquidTime() {
        lastLiquidTime = 0L;
    }

    public synchronized void setFlying(boolean flying) {
        if (!flying) {
            flying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && gameMode == GameMode.SPECTATOR;
        }
        this.flying = flying;

        if (flying) {
            this.flyingTime = System.currentTimeMillis() + 3_000L;
        }
    }

    public boolean hasShield() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && !Compatibility.CompatibilityType.OldCombatMechanics.isFunctional()
                && (PlayerData.hasItemInHands(this, Material.SHIELD)
                || isUsingItem());
    }

    public boolean isDead() {
        return dead;
    }

    public synchronized void setDead(boolean dead) {
        this.dead = dead;
    }

    public boolean isSleeping() {
        return sleeping;
    }

    public synchronized void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    public synchronized void setLastHeadMovement() {
        this.lastHeadMovement = System.currentTimeMillis();
    }

    public long getLastHeadMovement() {
        return System.currentTimeMillis() - lastHeadMovement;
    }

    public boolean isMoving(boolean head) {
        if (!head || getLastHeadMovement() <= 2_500L) {
            Double nmsDistance = getNmsDistance();
            return nmsDistance != null && nmsDistance >= 0.1
                    || getCustomDistance() >= 0.1
                    || isSprinting() || isSprintJumping() || isWalkJumping()
                    || Damage.getLastReceived(this) <= 100L;
        } else {
            return false;
        }
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public synchronized void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public synchronized void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

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

    public boolean refreshOnGroundCustom(SpartanLocation from) {
        if (PlayerData.isOnGround(this, this.getLocation(), 0.0)) {
            this.onGroundCustom = true; // Server Current Location
        } else if (from != null && PlayerData.isOnGround(this, from, 0.0)) {
            this.onGroundCustom = true; // Server Previous Location
        } else {
            SpartanLocation eventFrom = getEventFromLocation();

            Runnable runnable = () -> {
                if (PlayerData.isOnGround(this, eventFrom, 0.0)) {
                    this.onGroundCustom = true; // Event Previous Location
                } else {
                    SpartanLocation eventLocation = getEventToLocation();
                    this.onGroundCustom = eventLocation != null
                            && PlayerData.isOnGround(this, eventLocation, 0.0); // Event Current Location
                }
            };

            if (MultiVersion.folia) {
                SpartanBukkit.runTask(
                        eventFrom.getWorld(),
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

    public void removePotionEffect(PotionEffectType potionEffectType) {
        if (hasPotionEffect(potionEffectType)) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.removePotionEffect(potionEffectType);
                setActivePotionEffects(p.getActivePotionEffects());
            }
        }
    }

    public void addPotionEffect(PotionEffect potionEffect) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.addPotionEffect(potionEffect);
            setActivePotionEffects(p.getActivePotionEffects());
        }
    }

    public boolean canSee(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                return p.canSee(t);
            }
        }
        return true;
    }

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

            playerProfile.getPunishmentHistory().increaseKicks(this, kick);
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

            playerProfile.getPunishmentHistory().increaseWarnings(this, reason);
        }
    }

    public void damage(double amount, EntityDamageEvent.DamageCause damageCause) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            EntityDamageEvent event = new EntityDamageEvent(p, damageCause, amount);
            p.damage(amount);
            p.setLastDamageCause(event);
            setLastDamageCause(event, p.getMaximumNoDamageTicks());
        }
    }

    public PlayerProfile getProfile() {
        return playerProfile;
    }

    public synchronized void setProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public World getWorld() {
        return getLocation().getWorld();
    }

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

    public synchronized void setVehicle(Entity vehicle) {
        this.vehicle = vehicle;
    }

    public Entity getVehicle() {
        return vehicle;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public synchronized void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public synchronized void setFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public synchronized void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

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

    public double getHealth() {
        return health;
    }

    public synchronized void setHealth(double health) {
        this.health = health;
    }

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

    public GameMode getGameMode() {
        return gameMode;
    }

    public synchronized void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public double getEyeHeight() {
        return eyeHeight;
    }

    public synchronized void setEyeHeight(double eyeHeight) {
        this.eyeHeight = eyeHeight;
    }

    public boolean isBedrockPlayer() {
        return bedrockPlayer;
    }

    public boolean isGliding() {
        return gliding;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public synchronized void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public synchronized void setGliding(boolean gliding, boolean modify) {
        this.gliding = gliding;

        if (modify && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Player p = getPlayer();

            if (p != null) {
                p.setGliding(gliding);
            }
        }
    }

    public boolean isSwimming() {
        return swimming || swimmingTime >= System.currentTimeMillis();
    }

    public synchronized void setSwimming(boolean swimming, int ticks) {
        this.swimming = swimming;

        if (ticks != -1L) {
            this.swimmingTime = (ticks > 0 ? System.currentTimeMillis() + (ticks * 50L) : 0L);
        }
    }

    public ItemStack getItemInHand() {
        return inventory.getItemInHand();
    }

    public SpartanOpenInventory getOpenInventory() {
        return openInventory;
    }

    private void prepareForAbstractMovement() {
        setSprinting(false);
        setSneaking(false);
        setSwimming(false, 0);
        setGliding(false, false);
        removeLastLiquidTime();
    }

    public boolean teleport(SpartanLocation location) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.leaveVehicle();
            prepareForAbstractMovement();

            if (MultiVersion.folia) {
                p.teleportAsync(location.getBukkitLocation());
            } else {
                p.teleport(location.getBukkitLocation());
            }
            return true;
        }
        return false;
    }

    public synchronized void setVelocity(Vector vector) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.leaveVehicle();
            prepareForAbstractMovement();
            p.setVelocity(vector);
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public boolean isUsingItem() {
        return usingItem;
    }

    public synchronized void setUsingItem(boolean usingItem) {
        this.usingItem = usingItem;
    }

    public int getPing() {
        if (ping == -1) {
            Player p = this.getPlayer();
            return p != null ? (ping = Latency.ping(p)) : 0;
        }
        return ping;
    }

    public long getDamageTick() {
        return damageTick;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause;
    }

    public synchronized void setLastDamageCause(EntityDamageEvent lastDamageCause, int maximumNoDamageTicks) {
        this.damageTick = TPS.getTick(this);
        this.lastDamageCause = lastDamageCause;
        this.maximumNoDamageTicks = maximumNoDamageTicks;
    }

    public int getMaximumNoDamageTicks() {
        return maximumNoDamageTicks;
    }

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


    public boolean canSee(Player target) {
        if (SpartanBukkit.supportedFork) {
            Player player = getPlayer();

            if (player != null) {
                return player.canSee(target);
            }
        }
        return true;
    }

    public SpartanInventory getInventory() {
        return inventory;
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

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        double max = Math.max(Math.max(x, y), z);

        synchronized (nearbyEntities) {
            if (max > nearbyEntitiesDistance) {
                Player p = this.getPlayer();

                if (p != null) {
                    nearbyEntitiesDistance = max;

                    if (MultiVersion.folia || SpartanBukkit.isSynchronised()) {
                        SpartanBukkit.runTask(this, () -> {
                            nearbyEntities.clear();
                            nearbyEntities.addAll(p.getNearbyEntities(x, y, z));
                        });
                    } else {
                        SpartanLocation loc = this.getLocation();
                        List<Entity> entities;

                        synchronized (worldEntities) {
                            entities = worldEntities.get(loc.getWorld());
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

                                if (entity instanceof LivingEntity ? entity.isDead() : !(entity instanceof Vehicle)
                                        || loc.distance(entity.getLocation()) > max) {
                                    iterator.remove();
                                }
                            }
                            nearbyEntities.clear();
                            nearbyEntities.addAll(entities);
                        } else {
                            nearbyEntities.clear();
                        }
                    }
                } else {
                    nearbyEntities.clear();
                }
            } else if (!nearbyEntities.isEmpty()) {
                SpartanLocation loc = this.getLocation();
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
            }
            return nearbyEntities;
        }
    }

    public int getEnemiesNumber(double distance, boolean includeRest) {
        Player n = getPlayer();

        if (n != null) {
            List<Entity> entities = getNearbyEntities(distance, distance, distance);

            if (!entities.isEmpty()) {
                int count = 0;

                for (Entity e : getNearbyEntities(distance, distance, distance)) {
                    if (e instanceof Mob) {
                        LivingEntity target = ((Mob) e).getTarget();

                        if (target != null && target.equals(n)) {
                            count++;
                        }
                    } else if (e instanceof Player) {
                        SpartanPlayer target = SpartanBukkit.getPlayer((Player) e);

                        if (target != null
                                && playerProfile.getCombat().isActivelyFighting(
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
        if (!isSleeping()) {
            if (getWorld() == loc.getWorld()) {
                SpartanLocation to = getLocation();
                to.setX(loc.getX());
                to.setY(loc.getY());
                to.setZ(loc.getZ());
                return teleport(loc);
            }
            return teleport(getLocation());
        }
        return false;
    }

    public boolean groundTeleport(boolean checkGround) {
        boolean testServer = TestServer.isIdentified();

        if (!testServer && !Config.settings.getBoolean("Detections.ground_teleport_on_detection")) {
            return false;
        }
        SpartanLocation location = getLocation();

        if (checkGround
                && onGroundCustom
                && GroundUtils.isOnGround(this, location, 0.0, false, false)) {
            return false;
        }
        SpartanLocation locationP1 = location.clone().add(0, 1, 0);

        if (BlockUtils.isSolid(locationP1) && !(BlockUtils.areWalls(locationP1) || BlockUtils.canClimb(locationP1))) {
            return false;
        }
        World world = getWorld();
        float countedDamage = 0.0f;
        double startY = Math.min(BlockUtils.getMaxHeight(world), location.getY()),
                box = startY - Math.floor(startY);
        int iterations = 0,
                maxIterations = MoveUtils.chunkInt;

        if (!GroundUtils.heightExists(box)) {
            box = 0.0;
        }

        for (double progressiveY = startY; startY > BlockUtils.getMinHeight(world); progressiveY--) {
            SpartanLocation loopLocation = location.clone().add(0.0, -(startY - progressiveY), 0.0);
            Material material = loopLocation.getBlock().getType();

            if (iterations != maxIterations
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

        if (testServer
                || Config.settings.getBoolean("Detections.fall_damage_on_teleport")) { // Damage
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

    // Damage

    public void applyFallDamage(double d) {
        handlers.disable(Handlers.HandlerType.Floor, 3);
        handlers.disable(Handlers.HandlerType.Velocity, 3);
        damage(d, EntityDamageEvent.DamageCause.FALL);
    }

    // Ground

    public int getBlocksOffGround(int limit, boolean liquid, boolean climbable) {
        boolean hasLimit = limit > 0;
        SpartanLocation ploc = getLocation();
        Decimals decimals = getDecimals();
        Cooldowns cooldowns = getCooldowns();
        String key = "blocks-off-ground=" + Objects.hash(ploc.getBlockY(), hasLimit ? limit : 0, liquid, climbable);

        if (!cooldowns.canDo(key)) {
            return (int) decimals.get(key, 0.0);
        }
        int blocksOffGround = 0;

        for (double i = 0.0; i <= ploc.getY(); i++) {
            if (!GroundUtils.isOnGround(this, ploc.clone().add(0, -i, 0), -i, liquid, climbable)) {
                blocksOffGround++;

                if (hasLimit && blocksOffGround == limit) {
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

    // Jumping

    public boolean isJumping(double d) {
        double precision = MoveUtils.getJumpingPrecision(this);
        return MoveUtils.isJumping(
                d - getExtraMovementFromJumpEffect(),
                precision,
                bedrockPlayer ? precision : 0.0
        );
    }

    public long getLastJump() {
        return System.currentTimeMillis() - lastJump;
    }

    public synchronized void setLastJump() {
        this.lastJump = System.currentTimeMillis();
        removeLastLiquidTime();
        handlers.removeMany(Handlers.HandlerFamily.Velocity);

        if (!Liquid.isLocation(this, this.getLocation())) {
            this.removeLastLiquidTime();
        }
    }

    public boolean isFalling(double dy) {
        return MoveUtils.getFallingTick(dy) != -1;
    }

    public boolean isClimbing(double d) {
        double v5 = AlgebraUtils.cut(d, 5);

        if (v5 != MoveUtils.gravityAcceleration && v5 != 0.07544) {
            for (double value : MoveUtils.climbing) {
                if (Math.abs(value - Math.abs(d)) < MoveUtils.lowPrecision) {
                    return true;
                }
            }
        }
        return false;
    }

    public double getExtraMovementFromJumpEffect() {
        return PlayerData.getPotionLevel(this, PotionEffectType.JUMP) * 0.1;
    }

    // Allowance

    public boolean canDo(boolean bypassItemAttributes) {
        if ((bypassItemAttributes || !Attributes.has(this, Attributes.GENERIC_MOVEMENT_SPEED))
                && !isDead() && !isSleeping() && canRunChecks(true) && !wasFlying() && !Velocity.hasCooldown(this)) {
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

    public boolean canDoAccurately() {
        return !isDead() && !isFlying() && !isSleeping()
                && canRunChecks(false)
                && !Velocity.hasCooldown(this);
    }

    // Chasing

    // Environment

    public boolean stepsOnBoats() {
        return GroundUtils.stepsOnBoats(this);
    }

    // Falling

    public double getCalculatedFallingRatio(double vertical) {
        if (vertical < 0.0) {
            vertical -= MoveUtils.gravityAcceleration;
            double calculated = MoveUtils.gravityAcceleration * (getTicksOnAir() + 1);
            return Math.abs(calculated - Math.abs(vertical));
        }
        return 0.0;
    }

    public long getLastFall() {
        return System.currentTimeMillis() - lastFall;
    }

    public synchronized void setLastFall() {
        this.lastFall = System.currentTimeMillis();
    }

    // Base

    public void resetHandlers() {
        for (Handlers.HandlerType handlerType : Handlers.HandlerType.values()) {
            handlers.remove(handlerType);
        }
    }

    // Speed

    public long getLastOffSprint() {
        return System.currentTimeMillis() - lastOffSprint;
    }

    public synchronized void setLastOffSprint() {
        this.lastOffSprint = System.currentTimeMillis();
    }

    // Distance

    public double getCustomDistance() {
        return customDistance;
    }

    public synchronized void setCustomDistance(double distance,
                                               double horizontal,
                                               double vertical) {
        this.customDistance = distance;
        this.customHorizontalDistance = horizontal;
        this.customVerticalDistance = vertical;
    }

    public double getCustomHorizontalDistance() {
        return customHorizontalDistance;
    }

    public double getCustomVerticalDistance() {
        return customVerticalDistance;
    }

    private void clearNmsDistance(Map<Long, Double> map) {
        int size = map.size();

        if (size > 2) {
            size = size - 2;
            Iterator<Double> iterator = map.values().iterator();

            while (size > 0 && iterator.hasNext()) {
                size--;
                iterator.next();
                iterator.remove();
            }
        }
    }

    public synchronized void setNmsDistance(double distance,
                                            double horizontal,
                                            double vertical,
                                            double box) {
        long tick = TPS.getTick(this);
        this.nmsDistance.put(tick, distance);
        this.nmsHorizontalDistance.put(tick, horizontal);
        this.nmsVerticalDistance.put(tick, vertical);
        this.nmsBox.put(tick, box);
        clearNmsDistance(nmsDistance);
        clearNmsDistance(nmsHorizontalDistance);
        clearNmsDistance(nmsVerticalDistance);
        clearNmsDistance(nmsBox);
    }

    public double getValueOrDefault(Double value, double def) {
        return value == null ? def : value;
    }

    public synchronized Double getNmsDistance() {
        return nmsDistance.get(TPS.getTick(this));
    }

    public synchronized Double getPreviousNmsDistance() {
        return nmsDistance.get(TPS.getTick(this) - 1L);
    }

    public synchronized Double getNmsHorizontalDistance() {
        return nmsHorizontalDistance.get(TPS.getTick(this));
    }

    public synchronized Double getPreviousNmsHorizontalDistance() {
        return nmsHorizontalDistance.get(TPS.getTick(this) - 1L);
    }

    public synchronized Double getNmsVerticalDistance() {
        return nmsVerticalDistance.get(TPS.getTick(this));
    }

    public synchronized Double getPreviousNmsVerticalDistance() {
        return nmsVerticalDistance.get(TPS.getTick(this) - 1L);
    }

    public synchronized Double getNmsBox() {
        return nmsBox.get(TPS.getTick(this));
    }

    public synchronized Double getOld_NmsBox() {
        return nmsBox.get(TPS.getTick(this) - 1L);
    }

    // Direction

    public SpartanBlock getIllegalTargetBlock(SpartanBlock clickedBlock) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            SpartanLocation clickedBlockLocation = clickedBlock.getLocation();
            boolean editableClickedBlock = BlockUtils.isChangeable(clickedBlockLocation);

            if (!editableClickedBlock && !BlockUtils.isSolid(clickedBlockLocation)) {
                return null;
            }
            double distance = getLocation().distance(clickedBlockLocation);

            if (distance >= 0.5) {
                try {
                    SpartanBlock targetBlock = getTargetBlock(Math.max(Math.floor(distance), 1.0));

                    if (targetBlock != null && BlockUtils.isSolid(targetBlock.getType())) {
                        SpartanLocation targetBlockLocation = targetBlock.getLocation();

                        if (clickedBlockLocation.distance(targetBlockLocation) >= (handlers.has(Handlers.HandlerType.ElytraUse) ? 2.5 : BlockUtils.areEggs(getItemInHand().getType()) ? 2.0 : 1.0)

                                && (targetBlock.getX() != clickedBlock.getX()
                                || targetBlock.getY() != clickedBlock.getY()
                                || targetBlock.getZ() != clickedBlock.getZ())

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

    // Border

    public boolean isOutsideOfTheBorder(double deviation) {
        SpartanLocation loc = getLocation();
        WorldBorder border = getWorld().getWorldBorder();
        double size = (border.getSize() / 2.0) + deviation;
        Location center = border.getCenter();
        return Math.abs(loc.getX() - center.getX()) > size
                || Math.abs(loc.getZ() - center.getZ()) > size;
    }

    // Counters

    public int getExtraPackets() {
        return isUsingItem() ? 0 : extraPackets;
    }

    public synchronized void setExtraPackets(int number) {
        this.extraPackets = number;
    }

    public int getTicksOnAir() {
        return airTicks;
    }

    public synchronized void setAirTicks(int number) {
        if (airTicks > 0) {
            setOldAirTicks(airTicks);
        }
        this.airTicks = number;

        if (number == 0) {
            this.fallingTicks = 0;
        }
    }

    public int getOldTicksOnAir() {
        return oldAirTicks;
    }

    public synchronized void setOldAirTicks(int number) {
        this.oldAirTicks = number;
    }

    public int getFallingTicks() {
        return fallingTicks;
    }

    public synchronized void setFallingTicks(int number) {
        this.fallingTicks = number;
    }

    public int getTicksOnGround() {
        return groundTicks;
    }

    public synchronized void setGroundTicks(int number) {
        this.groundTicks = number;

        if (number > 0) {
            this.fallingTicks = 0;
        }
    }

    // Movement

    public boolean isWalkJumping() {
        return !sprinting && getLastJump() <= (TPS.tickTime * 2L);
    }

    public boolean isSprintJumping() {
        return sprinting && getLastJump() <= (TPS.tickTime * 2L);
    }

    public boolean isCrawling() {
        return getEyeHeight() <= 1.2 && !isGliding() && !isSwimming();
    }
}
