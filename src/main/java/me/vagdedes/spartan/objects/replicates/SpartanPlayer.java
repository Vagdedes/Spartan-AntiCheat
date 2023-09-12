package me.vagdedes.spartan.objects.replicates;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import me.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import me.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.compatibility.necessary.bedrock.BedrockCompatibility;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.performance.DetectionTick;
import me.vagdedes.spartan.features.performance.MaximumCheckedPlayers;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.features.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.features.protections.Teleport;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.handlers.connection.Latency;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.ElytraUse;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Velocity;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.interfaces.listeners.EventsHandler7;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.data.*;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.system.*;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpartanPlayer {

    private static final int tickFrequency = 5 * 1200;
    private static int syncTicks = 0;
    private static final Map<World, List<Entity>> worldEntities = new ConcurrentHashMap<>(Math.max(Bukkit.getWorlds().size(), 1));
    private static final Object
            nearbyEntitiesLock = new Object(),
            potionEffectLock = new Object();

    final String name;
    private final UUID uuid;
    private final String ipAddress;
    private boolean flying;
    private boolean allowFlight;
    private boolean dead;
    private boolean sleeping;
    private WeatherType playerWeather;
    private Collection<PotionEffect> potionEffects;
    private float walkSpeed;
    private float flySpeed;
    private SpartanInventory inventory;
    private boolean gliding;
    private boolean swimming;
    private boolean sprinting;
    private boolean sneaking;
    private boolean frozen;
    private double eyeHeight;
    private GameMode gameMode;
    private double health;
    private boolean invulnerable;
    private final boolean bedrockPlayer;
    private final ResearchEngine.DataType dataType;
    private final InetSocketAddress address;
    private final long creationTime;
    private final long lastPlayed;
    private final long firstPlayed;
    private boolean usingItem;
    private int fireTicks;
    private double lastDamage;
    private EntityDamageEvent lastDamageCause;
    private SpartanOpenInventory openInventory;
    private int ping;
    private int maximumNoDamageTicks;
    private float fallDistance;
    private int foodLevel;
    private boolean onGroundCustom;
    private long lastHeadMovement;

    // Data

    private final Buffer[] buffer;
    private final Timer[] timer;
    private final Decimals[] decimals;
    private final Tracker[] tracker;
    private final Cooldowns[] cooldowns;
    private final Handlers handlers;

    // Cache

    private PlayerProfile playerProfile;
    private final DetectionTick detectionTick;
    private final ClickData clickData;

    private SpartanLocation location;
    private long locationTime;
    private long chasing;

    private Entity vehicle;
    private List<Entity> nearbyEntities;
    private double nearbyEntitiesDistance;

    private final boolean[] runChecks;
    private final Map<Enums.HackType, Boolean> runCheck;
    private final Map<Enums.HackType, Boolean> runCheckAccountLag;

    // Custom

    private double
            nmsDistance, nmsHorizontalDistance, nmsVerticalDistance,
            oldNmsDistance, oldNmsHorizontalDistance, oldNmsVerticalDistance,
            customDistance, customHorizontalDistance, customVerticalDistance;
    private long
            lastOffSprint,
            lastFall,
            lastJump,
            walking, jumpWalking,
            sprintingTime, jumpSprinting,
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
        worldEntities.clear();
        SpartanLocation.memory.clear();
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
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
        List<Entity> collection = worldEntities.get(entity.getWorld());

        if (collection != null) {
            collection.add(entity);
        }
    }

    private static void run(boolean sync) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (sync) {
            boolean clearCache, hasPlayers = players.size() > 0;

            if (syncTicks == 0) {
                clearCache = true;
                syncTicks = tickFrequency;
                worldEntities.clear();

                if (hasPlayers) {
                    for (World world : Bukkit.getWorlds()) {
                        worldEntities.put(
                                world,
                                MultiVersion.folia ? world.getEntities() : new CopyOnWriteArrayList<>(world.getEntities())
                        );
                    }
                }
            } else {
                clearCache = false;
                syncTicks--;

                if (syncTicks % 20 == 0) {
                    worldEntities.clear();

                    if (hasPlayers) {
                        for (World world : Bukkit.getWorlds()) {
                            worldEntities.put(
                                    world,
                                    MultiVersion.folia ? world.getEntities() : new CopyOnWriteArrayList<>(world.getEntities())
                            );
                        }
                    }
                }
            }

            if (hasPlayers) {
                for (SpartanPlayer p : players) {
                    if (clearCache) {
                        Cache.clearCheckCache(p);
                    }
                    synchronized (nearbyEntitiesLock) {
                        p.nearbyEntities = null;
                    }
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
                        HackPreventionUtils.run(p);
                    });
                }
            }
        } else if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                SpartanBukkit.runTask(p, () -> {
                    if (p != null) {
                        // Ground Identification Cache
                        p.resetPing();
                        Player n = p.getPlayer();

                        if (n != null) {
                            // Only
                            p.setFireTicks(n.getFireTicks());
                            p.setFrozen(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && n.isFrozen());
                            p.setInvulnerable(p.isFrozen() || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && n.isInvulnerable());

                            // Bad
                            p.setPlayerWeather(n.getPlayerWeather());
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
                            p.setFlying(n.isFlying(), n.getAllowFlight());
                            p.setFoodLevel(n.getFoodLevel(), false);
                            p.setDead(n.isDead());
                            p.setLastDamageCause(n.getLastDamageCause(), n.getLastDamage(), n.getMaximumNoDamageTicks());

                            // Check Allowance Cache
                            if (TPS.getTick(p) <= 100
                                    || !MaximumCheckedPlayers.isChecked(p.getUniqueId())
                                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getGameMode() == GameMode.SPECTATOR
                                    || IDs.isPreview()
                                    || p.getCancellableCompatibility() != null) {
                                p.runChecks[0] = false;
                                p.runChecks[1] = false;
                            } else {
                                p.runChecks[0] = !LagLeniencies.hasInconsistencies(p, "");
                                p.runChecks[1] = true;
                            }

                            // External
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

        this.location = new SpartanLocation(this, p.getLocation());
        this.locationTime = 0L;
        this.nearbyEntities = null;
        this.nearbyEntitiesDistance = 0;
        this.runChecks = new boolean[]{false, false};
        this.runCheck = new LinkedHashMap<>(hackTypes.length);
        this.runCheckAccountLag = new LinkedHashMap<>(hackTypes.length);
        this.buffer = new Buffer[hackTypes.length + 1];
        this.timer = new Timer[hackTypes.length + 1];
        this.decimals = new Decimals[hackTypes.length + 1];
        this.tracker = new Tracker[hackTypes.length + 1];
        this.cooldowns = new Cooldowns[hackTypes.length + 1];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            boolean async = hackType.getCheck().canBeAsynchronous();
            this.buffer[id] = new Buffer(async);
            this.timer[id] = new Timer(async);
            this.decimals[id] = new Decimals(async);
            this.tracker[id] = new Tracker(async);
            this.cooldowns[id] = new Cooldowns(async);
        }
        this.buffer[hackTypes.length] = new Buffer(true);
        this.timer[hackTypes.length] = new Timer(true);
        this.decimals[hackTypes.length] = new Decimals(true);
        this.tracker[hackTypes.length] = new Tracker(true);
        this.cooldowns[hackTypes.length] = new Cooldowns(true);
        this.handlers = new Handlers();

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.uuid = uuid;
        this.flying = p.isFlying();
        this.allowFlight = p.getAllowFlight();
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
        this.lastDamage = p.getLastDamage();
        this.lastDamageCause = p.getLastDamageCause();
        this.openInventory = spartanOpenInv;
        this.maximumNoDamageTicks = p.getMaximumNoDamageTicks();
        this.fallDistance = p.getFallDistance();
        this.foodLevel = p.getFoodLevel();
        this.potionEffects = new ArrayList<>(p.getActivePotionEffects());
        this.playerWeather = p.getPlayerWeather();
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? ResearchEngine.DataType.Bedrock : ResearchEngine.DataType.Java;
        this.ping = Latency.ping(p);
        this.frozen = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && p.isFrozen();
        this.lastPlayed = p.getLastPlayed();
        this.firstPlayed = p.getFirstPlayed();
        this.customDistance = 0.0;
        this.customHorizontalDistance = 0.0;
        this.customVerticalDistance = 0.0;
        this.nmsDistance = 0.0;
        this.oldNmsDistance = 0.0;
        this.nmsHorizontalDistance = 0.0;
        this.oldNmsHorizontalDistance = 0.0;
        this.nmsVerticalDistance = 0.0;
        this.oldNmsVerticalDistance = 0.0;
        this.lastFall = 0L;
        this.lastOffSprint = 0L;
        this.lastJump = 0L;
        this.airTicks = 0;
        this.oldAirTicks = 0;
        this.fallingTicks = 0;
        this.groundTicks = 0;
        this.extraPackets = 0;
        this.sprintingTime = 0L;
        this.jumpSprinting = 0L;
        this.walking = 0L;
        this.jumpWalking = 0;
        this.swimmingTime = 0L;
        this.flyingTime = 0L;
        this.chasing = 0L;
        this.lastLiquidTime = 0L;
        this.lastHeadMovement = System.currentTimeMillis();
        this.detectionTick = new DetectionTick();
        this.playerProfile = ResearchEngine.getPlayerProfile(this);
        this.clickData = new ClickData();
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
    }

    // Separator

    public Buffer[] getBuffers() {
        return buffer;
    }

    public Timer[] getTimers() {
        return timer;
    }

    public Decimals[] getDecimalss() {
        return decimals;
    }

    public Tracker[] getTrackers() {
        return tracker;
    }

    public Cooldowns[] getCooldownsList() {
        return cooldowns;
    }

    public Buffer getBuffer() {
        return buffer[Enums.hackTypeLength];
    }

    public Timer getTimer() {
        return timer[Enums.hackTypeLength];
    }

    public Decimals getDecimals() {
        return decimals[Enums.hackTypeLength];
    }

    public Tracker getTracker() {
        return tracker[Enums.hackTypeLength];
    }

    public Cooldowns getCooldowns() {
        return cooldowns[Enums.hackTypeLength];
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

    public Handlers getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean canSkipDetectionTick(int ticksCooldown) {
        return detectionTick.canSkip(uuid, playerProfile, ticksCooldown, this);
    }

    public int getCPS() {
        return clickData.getCount();
    }

    public ClickData getClickData() {
        return clickData;
    }

    public void calculateClickData(Action action, boolean runRegardless) {
        if (!runRegardless) {
            if (PlayerData.isInActivePlayerCombat(this)) {
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
                clickData.calculate();
            } else if (action == null) {
                if (clickData.getLastCalculation() > 50L) {
                    success = true;
                    clickData.calculate();
                } else {
                    success = false;
                }
            } else {
                success = false;
            }

            if (success && clickData.canDistributeInformation()) {
                PlayerInfo.refresh(getName());
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

    public SpartanLocation getEventLocation(Location to) {
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

                if (players.size() > 0) {
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
                    && hackType.getCheck().isEnabled(this.getDataType(), this.getWorld().getName(), this.getUniqueId())
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

    public String getCancellableCompatibility() {
        return MythicMobs.is(this) ? Compatibility.CompatibilityType.MythicMobs.toString() :
                ItemsAdder.is(this) ? Compatibility.CompatibilityType.ItemsAdder.toString() :
                        CustomEnchantsPlus.has(this) ? Compatibility.CompatibilityType.CustomEnchantsPlus.toString() :
                                EcoEnchants.has(this) ? Compatibility.CompatibilityType.EcoEnchants.toString() : null;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public WeatherType getPlayerWeather() {
        return playerWeather;
    }

    public synchronized void setPlayerWeather(WeatherType weatherType) {
        playerWeather = weatherType;
    }

    public boolean isFlying() {
        Entity vehicle = getVehicle();
        return vehicle != null ? vehicle instanceof Player && ((Player) vehicle).isFlying() : flying;
    }

    public boolean getAllowFlight() {
        return allowFlight;
    }

    public boolean wasFlying() {
        return isFlying() || flyingTime >= System.currentTimeMillis();
    }

    public long getLastLiquidTime() {
        return lastLiquidTime;
    }

    public boolean wasInLiquids() {
        return System.currentTimeMillis() - lastLiquidTime <= 750L;
    }

    public synchronized void setLastLiquidTime() {
        lastLiquidTime = System.currentTimeMillis();
    }

    public synchronized void setFlying(boolean flying, boolean allowFlight) {
        if (!flying) {
            flying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && gameMode == GameMode.SPECTATOR;
        }
        this.flying = flying;
        this.allowFlight = allowFlight;

        if (flying) {
            if (gameMode == GameMode.CREATIVE) {
                handlers.add(Handlers.HandlerType.GameMode, 20);
            }
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
        return (!head || getLastHeadMovement() <= 2_500L)
                && (getNmsDistance() >= 0.1
                || getCustomDistance() >= 0.1
                || isSprinting() || isWalking()
                || isSprintJumping() || isWalkJumping()
                || Damage.getLastReceived(this) <= 100L);
    }

    public boolean isVanillaSprinting() {
        return sprinting;
    }

    public boolean isSprinting() {
        return sprinting || isCustomSprinting();
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
        Player p = this.getPlayer();
        return p != null && p.isOnGround();
    }

    public boolean refreshOnGroundCustom(SpartanLocation from) {
        if (!TestServer.isIdentified() && this.isOnGround()

                && (Settings.getBoolean("Performance.use_vanilla_ground_method")
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                || LagLeniencies.hasInconsistencies(this, "tps"))) {
            this.onGroundCustom = true; // Vanilla On-Ground When Necessary
        } else if (PlayerData.isOnGround(this, this.getLocation(), 0.0)) {
            this.onGroundCustom = true; // Server Current Location
        } else if (from != null && PlayerData.isOnGround(this, from, 0.0)) {
            this.onGroundCustom = true; // Server Previous Location
        } else {
            PlayerMoveEvent event = EventsHandler7.getMovementEvent(this);

            if (event != null) {
                Location eventFrom = event.getFrom();
                Runnable runnable = () -> {
                    if (PlayerData.isOnGround(this, new SpartanLocation(this, eventFrom), 0.0)) {
                        this.onGroundCustom = true; // Event Previous Location
                    } else {
                        Location eventLocation = event.getTo();
                        this.onGroundCustom = eventLocation != null && PlayerData.isOnGround(this, new SpartanLocation(this, eventLocation), 0.0); // Event Current Location
                    }
                };

                if (MultiVersion.folia) {
                    SpartanBukkit.runTask(
                            this,
                            eventFrom.getWorld(),
                            eventFrom.getBlockX() >> 4,
                            eventFrom.getBlockZ() >> 4,
                            runnable
                    );
                } else {
                    runnable.run();
                }
            } else {
                this.onGroundCustom = false;
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

    public void showPlayer(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                p.showPlayer(t);
            }
        }
    }

    public void hidePlayer(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                p.hidePlayer(t);
            }
        }
    }

    public void kickPlayer(String reason) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.kickPlayer(reason);
        }
    }

    public void damage(double amount, EntityDamageEvent.DamageCause damageCause) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            EntityDamageEvent event = new EntityDamageEvent(p, damageCause, amount);
            p.damage(amount);
            p.setLastDamageCause(event);
            setLastDamageCause(event, amount, p.getMaximumNoDamageTicks());
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

    public long getFirstPlayed() {
        return firstPlayed;
    }

    public boolean isUsingItem() {
        return usingItem;
    }

    public synchronized void setUsingItem(boolean usingItem) {
        this.usingItem = usingItem;
    }

    public void resetPing() {
        ping = -1;
    }

    public int getPing() {
        if (ping == -1) {
            Player p = this.getPlayer();
            return p != null ? (ping = Latency.ping(p)) : 0;
        }
        return ping;
    }

    public double getLastDamage() {
        return lastDamage;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause == null ? new EntityDamageEvent(getPlayer(), EntityDamageEvent.DamageCause.CUSTOM, 0) : lastDamageCause;
    }

    public synchronized void setLastDamageCause(EntityDamageEvent lastDamageCause, double lastDamage, int maximumNoDamageTicks) {
        this.lastDamageCause = lastDamageCause;
        this.lastDamage = lastDamage;
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

    public Collection<PotionEffect> getActivePotionEffects() {
        synchronized (potionEffectLock) {
            return new ArrayList<>(potionEffects);
        }
    }

    public void setActivePotionEffects(Collection<PotionEffect> potionEffects) {
        synchronized (potionEffectLock) {
            this.potionEffects.clear();
            this.potionEffects.addAll(potionEffects);
        }
    }

    public PotionEffect getPotionEffect(PotionEffectType type) {
        if (!potionEffects.isEmpty()) {
            synchronized (potionEffectLock) {
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
        if (!potionEffects.isEmpty()) {
            synchronized (potionEffectLock) {
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
        if (!potionEffects.isEmpty()) {
            synchronized (potionEffectLock) {
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
        x = Math.max(Math.max(x, y), z);

        synchronized (nearbyEntitiesLock) {
            boolean isNull = nearbyEntities == null;

            // Caching
            if (isNull || x > nearbyEntitiesDistance) {
                Player p = this.getPlayer();

                if (p != null) {
                    nearbyEntitiesDistance = x;

                    if (SpartanBukkit.isSynchronised()) {
                        if (isNull) {
                            return nearbyEntities = new CopyOnWriteArrayList<>(p.getNearbyEntities(x, y, z));
                        } else {
                            nearbyEntities.clear();
                            nearbyEntities.addAll(p.getNearbyEntities(x, y, z));
                            return nearbyEntities;
                        }
                    } else {
                        SpartanLocation loc = this.getLocation();
                        List<Entity> entities = new ArrayList<>(worldEntities.getOrDefault(loc.getWorld(), new ArrayList<>(0)));

                        if (!entities.isEmpty()) {
                            Entity vehicle = this.getVehicle();

                            if (vehicle != null) {
                                entities.remove(vehicle);
                            }
                            entities.remove(p);
                            Iterator<Entity> iterator = entities.iterator();

                            while (iterator.hasNext()) {
                                Entity entity = iterator.next();

                                if (entity instanceof LivingEntity ? entity.isDead() : !(entity instanceof Vehicle)
                                        || loc.distance(entity.getLocation()) > x) {
                                    iterator.remove();
                                }
                            }
                        }
                        if (isNull) {
                            return nearbyEntities = new CopyOnWriteArrayList<>(entities);
                        } else {
                            nearbyEntities.clear();
                            nearbyEntities.addAll(entities);
                            return nearbyEntities;
                        }
                    }
                } else {
                    return new ArrayList<>(0);
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
                            || loc.distance(entity.getLocation()) > x) {
                        iterator.remove();
                    }
                }
                nearbyEntities.clear();
                nearbyEntities.addAll(entities);
                return nearbyEntities;
            } else {
                return nearbyEntities;
            }
        }
    }

    public boolean hasNearbyEntities(double x, double y, double z) {
        return !getNearbyEntities(x, y, z).isEmpty();
    }

    // Custom

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

        if (!testServer && !Settings.getBoolean("Detections.ground_teleport_on_detection")) {
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
                || Settings.getBoolean("Detections.fall_damage_on_teleport")) { // Damage
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
        handlers.disable(Handlers.HandlerType.Floor, Teleport.ticks);
        handlers.disable(Handlers.HandlerType.Velocity, Teleport.ticks);
        handlers.disable(Handlers.HandlerType.Teleport, 1);
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

    public double getJumpEffectLimit(double v) {
        double additional = getExtraMovementFromJumpEffect();

        if (additional > 0.0) {
            v += (additional + MoveUtils.highPrecision);
        }
        return v;
    }

    // Allowance

    public boolean canDo(boolean bypassItemAttributes) {
        if ((bypassItemAttributes || !Attributes.has(this, Attributes.GENERIC_MOVEMENT_SPEED))
                && !isDead() && !isSleeping() && canRunChecks(true) && !wasFlying() && !Velocity.hasCooldown(this)) {
            if (Compatibility.CompatibilityType.MythicMobs.isFunctional() || Compatibility.CompatibilityType.ItemsAdder.isFunctional()) {
                List<Entity> entities = getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (entities.size() > 0) {
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

    public boolean isChasing() {
        if (chasing >= System.currentTimeMillis()) {
            return true;
        }
        if (!playerProfile.isHacker()
                && isMoving(false) // Make sure the chaser is moving
                && CombatProcessing.getDecimal(this, CombatProcessing.yawDifference, 0.0) <= 45.0
                && CombatProcessing.getDecimal(this, CombatProcessing.pitchDifference, 0.0) <= 22.5) {
            List<Entity> entities = getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

            if (entities.size() > 0) { // Make sure there are entities nearby in a hit distance
                for (Entity entity : entities) {
                    if (entity instanceof Player) { // Make sure the entity is a player
                        SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                        if (target != null && target.isMoving(false)) { // Make sure the target is also moving
                            PlayerFight fight = playerProfile.getCombat().getCurrentFight(target);
                            PlayerOpponent[] playerOpponents = fight.getOpponent(this);

                            if (playerOpponents[0].getLastHit() <= PlayerData.combatTimeRequirement // Make sure the player has recently hit the target
                                    && (playerOpponents[0].hasHitCombo() || !CombatUtils.areFacingEachOther(this, (LivingEntity) entity)) // Make sure the player has a hit combo or is facing a different direction
                                    && playerOpponents[1].getLastDamage() <= PlayerData.combatTimeRequirement) {  // Make sure the target has recently received damage by the player
                                double[] utility = CombatUtils.get_X_Y_Distance(this, (LivingEntity) entity);

                                if (utility != null) {
                                    double distance = utility[2];

                                    if (distance >= CombatUtils.maxLegitimateHitDistance) { // Make sure the distance of the two players is beyond legitimacy
                                        chasing = System.currentTimeMillis() + PlayerData.combatTimeRequirement;
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

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

    public int getSneakingCount() {
        return getBuffer().get("sneaking-counter");
    }

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

    public synchronized void setCustomDistance(double distance) {
        this.customDistance = distance;
    }

    public double getCustomHorizontalDistance() {
        return customHorizontalDistance;
    }

    public synchronized void setCustomHorizontalDistance(double distance) {
        this.customHorizontalDistance = distance;
    }

    public double getCustomVerticalDistance() {
        return customVerticalDistance;
    }

    public synchronized void setCustomVerticalDistance(double distance) {
        this.customVerticalDistance = distance;
    }

    public double getNmsDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsDistance;
    }

    public synchronized void setNmsDistance(double distance) {
        this.oldNmsDistance = this.nmsDistance;
        this.nmsDistance = distance;
    }

    public double getOld_NmsDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : oldNmsDistance;
    }

    public double getNmsHorizontalDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsHorizontalDistance;
    }

    public synchronized void setNmsHorizontalDistance(double distance) {
        this.oldNmsHorizontalDistance = this.nmsHorizontalDistance;
        this.nmsHorizontalDistance = distance;
    }

    public double getNmsVerticalDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsVerticalDistance;
    }

    public synchronized void setNmsVerticalDistance(double distance) {
        this.oldNmsVerticalDistance = this.nmsVerticalDistance;
        this.nmsVerticalDistance = distance;
    }

    public double getOld_NmsVerticalDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : oldNmsVerticalDistance;
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

    public boolean isOutsideOfTheBorder() {
        SpartanLocation loc = getLocation();
        WorldBorder border = getWorld().getWorldBorder();
        double size = border.getSize() / 2.0;
        Location center = border.getCenter();
        return Math.abs(loc.getX() - center.getX()) > size
                || Math.abs(loc.getZ() - center.getZ()) > size;
    }

    public boolean isInTheBorder() {
        SpartanLocation loc = getLocation();
        WorldBorder border = getWorld().getWorldBorder();
        double sizeMinus = (border.getSize() / 2.0) - 1.0,
                sizePlus = sizeMinus + 2.0;
        Location center = border.getCenter();
        double x = Math.abs(loc.getX() - center.getX()),
                z = Math.abs(loc.getZ() - center.getZ());
        return x >= sizeMinus && x <= sizePlus
                || z >= sizeMinus && z <= sizePlus;
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

    public boolean isCustomSprinting() {
        return sprintingTime > System.currentTimeMillis();
    }

    public synchronized void setSprinting(int ticks) {
        this.sprintingTime = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isWalking() {
        return walking > System.currentTimeMillis();
    }

    public synchronized void setWalking(int ticks) {
        this.walking = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isWalkJumping() {
        return jumpWalking > System.currentTimeMillis();
    }

    public synchronized void setJumpWalking(int ticks) {
        this.jumpWalking = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isSprintJumping() {
        return jumpSprinting > System.currentTimeMillis();
    }

    public synchronized void setJumpSprinting(int ticks) {
        this.jumpSprinting = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isCrawling() {
        return getEyeHeight() <= 1.2 && !isGliding() && !isSwimming();
    }
}
