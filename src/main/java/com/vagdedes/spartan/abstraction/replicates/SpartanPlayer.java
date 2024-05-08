package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.CheckExecutorExample;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.data.*;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.Elytra;
import com.vagdedes.spartan.listeners.EventsHandler7;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
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
    private boolean frozen, onGroundCustom, dead, sleeping;
    private int ping, maximumNoDamageTicks, foodLevel;
    private double eyeHeight, health;
    private GameMode gameMode;
    public final boolean bedrockPlayer;
    public final long creationTime;
    public final String name;
    public final UUID uuid;
    public final String ipAddress;
    private final Map<PotionEffectType, SpartanPotionEffect> potionEffects;
    public final Enums.DataType dataType;
    private SpartanInventory inventory;
    private SpartanOpenInventory openInventory;
    public final SpartanPlayerMovement movement;
    public final SpartanPunishments punishments;
    private final Map<EntityDamageEvent.DamageCause, SpartanPlayerDamage> damageReceived, damageDealt;
    private final List<SpartanPlayerVelocity> velocityReceived;
    private SpartanPlayerDamage lastDamageReceived, lastDamageDealt;

    // Data

    private final Buffer[] buffer;
    private final Timer[] timer;
    private final Decimals[] decimals;
    private final Cooldowns[] cooldowns;
    private final CheckExecutor[] executors;
    private final LiveViolation[] violations;
    final Trackers trackers;
    private LiveViolation lastViolation;

    // Cache

    private PlayerProfile playerProfile;
    private final Clicks clicks;

    private Entity vehicle;
    private final List<Entity> nearbyEntities;
    private double nearbyEntitiesDistance;

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
                for (Cooldowns cooldowns : p.cooldowns) {
                    cooldowns.clear();
                }
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
                            }
                        }
                    }
                    if (p.refreshOnGround(new SpartanLocation[]{p.movement.getLocation()})) {
                        p.movement.resetAirTicks();
                    }
                    if (p.isOnGroundDefault()) {
                        p.movement.resetVanillaAirTicks();
                    }
                    p.nearbyEntitiesDistance = 0;

                    // Separator
                    SpartanBukkit.runTask(p, () -> {
                        Player n = p.getPlayer();

                        if (n != null) {
                            Collection<PotionEffect> potionEffects = n.getActivePotionEffects();
                            p.setStoredPotionEffects(potionEffects); // Bad
                            SpartanLocation to = p.movement.getLocation(),
                                    from = p.movement.getCustomFromLocation();

                            if (from != null) {
                                p.movement.setCustomDistance(
                                        to.distance(from),
                                        AlgebraUtils.getHorizontalDistance(to, from),
                                        to.getY() - from.getY()
                                );
                            }
                            p.movement.customFromLocation = to;

                            for (CheckExecutor executor : p.executors) {
                                executor.scheduler();
                            }
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

                            // Bad
                            p.setHealth(n.getHealth());
                            p.setSleeping(n.isSleeping());
                            p.setInventory(n.getInventory(), n.getOpenInventory());

                            // Good
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                                Elytra.judge(p, n.isGliding(), false);
                            }
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                                p.movement.setSwimming(n.isSwimming(), 0);
                            }
                            p.setGameMode(n.getGameMode());
                            p.movement.setFlying(n.isFlying());
                            p.setFoodLevel(n.getFoodLevel(), false);
                            p.setDead(n.isDead());
                            p.maximumNoDamageTicks = n.getMaximumNoDamageTicks();
                            p.playerProfile.playerCombat.track();
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
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? Enums.DataType.BEDROCK : Enums.DataType.JAVA;
        this.trackers = new Trackers(this);

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.dead = p.isDead();
        this.sleeping = p.isSleeping();
        this.walkSpeed = p.getWalkSpeed();
        this.eyeHeight = p.getEyeHeight();
        this.flySpeed = p.getFlySpeed();
        this.gameMode = p.getGameMode();
        this.health = p.getHealth();
        this.openInventory = spartanOpenInv;
        this.maximumNoDamageTicks = p.getMaximumNoDamageTicks();
        this.fallDistance = p.getFallDistance();
        this.foodLevel = p.getFoodLevel();
        this.potionEffects = Collections.synchronizedMap(
                SpartanPotionEffect.mapFromBukkit(this, p.getActivePotionEffects())
        );
        this.ping = Latency.ping(p);
        this.frozen = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && p.isFrozen();
        this.playerProfile = ResearchEngine.getPlayerProfile(this);
        this.clicks = new Clicks();
        this.vehicle = p.getVehicle();
        this.movement = new SpartanPlayerMovement(this, p);
        this.punishments = new SpartanPunishments(this);
        this.velocityReceived = Collections.synchronizedList(new LinkedList<>());
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
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, inv.getItemInOffHand());
        } else {
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, null);
        }
        this.creationTime = System.currentTimeMillis();

        this.buffer = new Buffer[hackTypes.length + 1];
        this.timer = new Timer[hackTypes.length + 1];
        this.decimals = new Decimals[hackTypes.length + 1];
        this.cooldowns = new Cooldowns[hackTypes.length + 1];
        this.executors = new CheckExecutor[hackTypes.length];
        this.violations = new LiveViolation[hackTypes.length];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            this.buffer[id] = new Buffer(this);
            this.timer[id] = new Timer();
            this.decimals[id] = new Decimals();
            this.cooldowns[id] = new Cooldowns(this);
            this.violations[id] = new LiveViolation(this, hackType);
            this.executors[id] = new CheckExecutorExample(this);
        }
        if (CheckExecutorExample.executors.length > 0) {
            for (String executorClass : CheckExecutorExample.executors) {
                try {
                    CheckExecutor executor = (CheckExecutor) Class.forName(executorClass).getConstructor(SpartanPlayer.class).newInstance(this);
                    this.executors[executor.hackType.ordinal()] = executor;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        this.lastViolation = this.violations[0];
        this.buffer[hackTypes.length] = new Buffer(this);
        this.timer[hackTypes.length] = new Timer();
        this.decimals[hackTypes.length] = new Decimals();
        this.cooldowns[hackTypes.length] = new Cooldowns(this);
    }

    public boolean debug(boolean function, boolean broadcast, boolean cutDecimals, Object... message) {
        if (function && SpartanBukkit.testMode) {
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

    public Cooldowns getCooldowns(Enums.HackType hackType) {
        return cooldowns[hackType.ordinal()];
    }

    public CheckExecutor getExecutor(Enums.HackType hackType) {
        return executors[hackType.ordinal()];
    }

    // Separator

    public Trackers getTrackers() {
        return trackers;
    }

    public void resetHandlers() {
        for (Trackers.TrackerType handlerType : Trackers.TrackerType.values()) {
            trackers.remove(handlerType);
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

    public boolean isOnFire() {
        return !this.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE, 0)
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

    public boolean isOnGround() {
        return onGroundCustom;
    }

    public boolean isOnGround(SpartanLocation location) {
        return GroundUtils.isOnGround(this, location, 0.0, false, true);
    }

    public boolean isOnGroundDefault() {
        Entity vehicle = getVehicle();

        if (vehicle != null) {
            return vehicle.isOnGround();
        } else {
            Player p = this.getPlayer();
            return p != null && p.isOnGround();
        }
    }

    public boolean refreshOnGround(SpartanLocation[] locations) {
        for (SpartanLocation location : locations) {
            if (GroundUtils.isOnGround(this, location, 0.0, false, true)) {
                return this.onGroundCustom = true;
            }
        }
        return this.onGroundCustom = false;
    }

    // Separator

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

    public SpartanBlock getIllegalTargetBlock(SpartanBlock clickedBlock) { // todo improve
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            SpartanLocation clickedBlockLocation = clickedBlock.getLocation();
            boolean editableClickedBlock = BlockUtils.isChangeable(clickedBlockLocation.getBlock().material);

            if (!editableClickedBlock
                    && !BlockUtils.isSolid(clickedBlockLocation.getBlock().material)) {
                return null;
            }
            double distance = movement.getLocation().distance(clickedBlockLocation);

            if (distance >= 0.5) {
                try {
                    SpartanBlock targetBlock = getTargetBlock(Math.max(Math.floor(distance), 1.0));

                    if (targetBlock != null && BlockUtils.isSolid(targetBlock.material)) {
                        SpartanLocation targetBlockLocation = targetBlock.getLocation();

                        if (clickedBlockLocation.distance(targetBlockLocation) >= (trackers.has(Trackers.TrackerType.ELYTRA_USE) ? 2.5 : BlockUtils.areEggs(getItemInHand().getType()) ? 2.0 : 1.0)

                                && (targetBlock.x != clickedBlock.x
                                || targetBlock.y != clickedBlock.y
                                || targetBlock.z != clickedBlock.z)

                                && (editableClickedBlock && !targetBlockLocation.getBlock().isLiquidOrWaterLogged(true)
                                || BlockUtils.isFullSolid(targetBlockLocation.getBlock().material))) {
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

    public SpartanPlayerVelocity getLastVelocityReceived() {
        if (velocityReceived.isEmpty()) {
            return null;
        } else {
            SpartanPlayerVelocity velocity;

            synchronized (this.velocityReceived) {
                velocity = this.velocityReceived.get(this.velocityReceived.size() - 1);
            }
            return velocity;
        }
    }

    public List<SpartanPlayerVelocity> getVelocitiesReceived() {
        synchronized (this.velocityReceived) {
            return new ArrayList<>(this.velocityReceived);
        }
    }

    public void addReceivedVelocity(PlayerVelocityEvent event) {
        if (this.velocityReceived.size() == TPS.maximum) {
            synchronized (this.velocityReceived) {
                this.velocityReceived.remove(0);
                this.velocityReceived.add(new SpartanPlayerVelocity(
                        this,
                        event,
                        this.movement.getLocation()
                ));
            }
        } else {
            synchronized (this.velocityReceived) {
                this.velocityReceived.add(
                        new SpartanPlayerVelocity(
                                this,
                                event,
                                this.movement.getLocation()
                        )
                );
            }
        }
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

    public boolean hasActivePotionEffects() {
        Entity vehicle = getVehicle();

        if (vehicle != null) {
            if (vehicle instanceof LivingEntity) {
                return !((LivingEntity) vehicle).getActivePotionEffects().isEmpty();
            } else {
                return false;
            }
        } else {
            synchronized (this.potionEffects) {
                return !this.potionEffects.isEmpty()
                        && this.potionEffects.values().stream().anyMatch(SpartanPotionEffect::isActive);
            }
        }
    }

    public Collection<SpartanPotionEffect> getStoredPotionEffects() {
        Entity vehicle = getVehicle();

        if (vehicle != null) {
            if (vehicle instanceof LivingEntity) {
                return SpartanPotionEffect.listFromBukkit(this, ((LivingEntity) vehicle).getActivePotionEffects());
            } else {
                return new ArrayList<>(0);
            }
        } else {
            synchronized (this.potionEffects) {
                return this.potionEffects.values();
            }
        }
    }

    public void setStoredPotionEffects(Collection<PotionEffect> effects) {
        synchronized (this.potionEffects) {
            for (PotionEffect effect : effects) {
                this.potionEffects.put(effect.getType(), new SpartanPotionEffect(this, effect));
            }
        }
    }

    public void removePotionEffect(PotionEffectType potionEffectType) {
        if (this.hasPotionEffect(potionEffectType, 0)) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.removePotionEffect(potionEffectType);
                setStoredPotionEffects(p.getActivePotionEffects());
            }
        }
    }

    public SpartanPotionEffect getPotionEffect(PotionEffectType type, long lastActive) {
        SpartanPotionEffect potionEffect;

        synchronized (this.potionEffects) {
            potionEffect = this.potionEffects.get(type);
        }
        if (potionEffect != null
                && potionEffect.ticksPassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type)) {
            return potionEffect;
        } else {
            return null;
        }
    }

    public boolean hasPotionEffect(PotionEffectType type, long lastActive) {
        SpartanPotionEffect potionEffect;

        synchronized (this.potionEffects) {
            potionEffect = this.potionEffects.get(type);
        }
        return potionEffect != null
                && potionEffect.ticksPassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type);
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

    // Teleport

    public boolean groundTeleport() {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection") || this.onGroundCustom) {
            return false;
        }
        SpartanLocation location = this.movement.getLocation(),
                locationP1 = location.clone().add(0, 1, 0);

        if (BlockUtils.isSolid(locationP1.getBlock().material)
                && !(BlockUtils.areWalls(locationP1.getBlock().material)
                || BlockUtils.canClimb(locationP1.getBlock().material))) {
            return false;
        }
        World world = getWorld();
        float countedDamage = 0.0f;
        double startY = Math.min(BlockUtils.getMaxHeight(world), location.getY()),
                box = startY - Math.floor(startY);
        int iterations = 0;

        if (!GroundUtils.blockHeightExists(box)) {
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
                teleport(loopLocation);
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

    public boolean teleport(SpartanLocation location) {
        if (this.getWorld().equals(location.world)) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                if (SpartanBukkit.isSynchronised()) {
                    p.leaveVehicle();
                }
                this.movement.setSprinting(false);
                this.movement.setSneaking(false);
                this.movement.setSwimming(false, 0);
                this.movement.setGliding(false, false);
                this.movement.removeLastLiquidTime();
                this.trackers.removeMany(Trackers.TrackerFamily.VELOCITY);

                if (MultiVersion.folia) {
                    p.teleportAsync(location.getBukkitLocation());
                } else {
                    p.teleport(location.getBukkitLocation());
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Separator

    public boolean applyFallDamage(double d) {
        trackers.disable(Trackers.TrackerType.ABSTRACT_VELOCITY, 3);
        return this.damage(d, EntityDamageEvent.DamageCause.FALL);
    }

    private boolean damage(double amount, EntityDamageEvent.DamageCause damageCause) {
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

    public String getCancellableCompatibility() {
        return MythicMobs.is(this) ? Compatibility.CompatibilityType.MYTHIC_MOBS.toString() :
                ItemsAdder.is(this) ? Compatibility.CompatibilityType.ITEMS_ADDER.toString() :
                        CustomEnchantsPlus.has(this) ? Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS.toString() :
                                EcoEnchants.has(this) ? Compatibility.CompatibilityType.ECO_ENCHANTS.toString() : null;
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
