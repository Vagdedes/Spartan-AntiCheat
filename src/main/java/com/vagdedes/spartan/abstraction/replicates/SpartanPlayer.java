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

    private static int schedulerTicks = 0;
    private static final Map<World, List<Entity>> worldEntities
            = Collections.synchronizedMap(new LinkedHashMap<>(Math.max(Bukkit.getWorlds().size(), 1)));

    public final boolean bedrockPlayer;
    boolean onGroundCustom;
    private final long creationTime;
    public final String name;
    public final UUID uuid;
    public final String ipAddress;
    private final Map<PotionEffectType, SpartanPotionEffect> potionEffects;
    public final Enums.DataType dataType;
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
    private final Trackers trackers;
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
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                schedulerTicks++;

                if (!MultiVersion.folia && schedulerTicks % 20 == 0) {
                    synchronized (worldEntities) {
                        worldEntities.clear();

                        if (!players.isEmpty()) {
                            for (World world : Bukkit.getWorlds()) {
                                worldEntities.put(
                                        world,
                                        new ArrayList<>(world.getEntities())
                                );
                            }
                        }
                    }
                }

                if (!players.isEmpty()) {
                    for (SpartanPlayer p : players) {
                        p.movement.judgeGround(p.movement.getLocation());

                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                            Elytra.judge(p, false);
                        }
                        p.playerProfile.playerCombat.track();
                        p.nearbyEntitiesDistance = 0;
                        p.movement.setDetectionLocation(false);

                        // Separator
                        SpartanBukkit.runTask(p, () -> {
                            Player n = p.getPlayer();

                            if (n != null) {
                                Collection<PotionEffect> potionEffects = n.getActivePotionEffects();
                                p.setStoredPotionEffects(potionEffects); // Bad
                                SpartanLocation to = p.movement.getLocation(),
                                        from = p.movement.getSchedulerFromLocation();

                                if (from != null) {
                                    p.movement.schedulerDistance = to.distance(from);
                                }
                                p.movement.schedulerFrom = to;

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
            }, 1L, 1L);
        }
    }

    // Separator

    public static void cacheEntity(Entity entity) {
        synchronized (worldEntities) {
            List<Entity> collection = worldEntities.get(entity.getWorld());

            if (collection != null) {
                collection.add(entity);
            }
        }
    }

    // Object

    public SpartanPlayer(Player p, UUID uuid) {
        Enums.HackType[] hackTypes = Enums.HackType.values();

        this.uuid = uuid;
        this.nearbyEntities = Collections.synchronizedList(new ArrayList<>());
        this.nearbyEntitiesDistance = 0;
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? Enums.DataType.BEDROCK : Enums.DataType.JAVA;
        this.trackers = new Trackers(this);

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.potionEffects = Collections.synchronizedMap(
                SpartanPotionEffect.mapFromBukkit(this, p.getActivePotionEffects())
        );
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

            if (p != null) {
                p.openInventory(inventory);
            }
        }
    }

    public Inventory createInventory(int size, String title) {
        Player p = getPlayer();

        if (p != null) {
            return Bukkit.createInventory(p, size, title);
        }
        return null;
    }

    public InventoryView getOpenInventory() {
        Player p = getPlayer();
        return p == null ? null : p.getOpenInventory();
    }

    // Separator

    public PlayerInventory getInventory() {
        Player p = getPlayer();
        return p == null ? null : p.getInventory();
    }

    public ItemStack getItemInHand() {
        PlayerInventory inventory = getInventory();
        return inventory == null ? new ItemStack(Material.AIR) : inventory.getItemInHand();
    }

    // Separator

    public boolean isOnFire() {
        return !this.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE, 0)
                && (this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE).ticksPassed() <= 5
                || this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE_TICK).ticksPassed() <= 5);
    }

    // Separator

    public Player getPlayer() {
        return SpartanBukkit.getRealPlayer(uuid);
    }

    public long timePassedSinceCreation() {
        return System.currentTimeMillis() - creationTime;
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
        Player p = getPlayer();
        return p != null && p.isDead();
    }

    // Separator

    public boolean isSleeping() {
        Player p = getPlayer();
        return p != null && p.isSleeping();
    }

    // Separator

    public boolean isOnGround() {
        return onGroundCustom;
    }

    public boolean isOnGround(SpartanLocation location) {
        return GroundUtils.isOnGround(this, location, 0.0);
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

    // Separator

    public PlayerProfile getProfile() {
        return playerProfile;
    }

    public void setProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    // Separator

    public SpartanBlock getTargetBlock(double distance) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            try {
                Player n = getPlayer();

                if (n != null) {
                    List<Block> list = n.getLineOfSight(null, (int) distance);

                    if (!list.isEmpty()) {
                        for (Block block : list) {
                            if (BlockUtils.isFullSolid(block.getType())) {
                                return new SpartanBlock(this, block);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // Separator

    public Entity getVehicle() {
        Player p = getPlayer();
        return p == null ? null : p.getVehicle();
    }

    // Separator

    public float getWalkSpeed() {
        Player p = getPlayer();
        return p == null ? 0.0f : p.getWalkSpeed();
    }

    // Separator

    public float getFlySpeed() {
        Player p = getPlayer();
        return p == null ? 0.0f : p.getFlySpeed();
    }

    // Separator

    public int getFoodLevel() {
        Player p = getPlayer();
        return p == null ? 0 : p.getFoodLevel();
    }

    public void setFoodLevel(int foodLevel) {
        Player p = getPlayer();

        if (p != null) {
            p.setFoodLevel(foodLevel);
        }
    }

    // Separator

    public double getHealth() {
        Player p = getPlayer();
        return p == null ? 0.0 : p.getHealth();
    }

    // Separator

    public float getFallDistance() {
        Player p = getPlayer();
        return p == null ? 0.0f : p.getFallDistance();
    }

    public void setFallDistance(float fallDistance) {
        Player p = getPlayer();

        if (p != null) {
            p.setFallDistance(fallDistance);
        }
    }

    // Separator

    public GameMode getGameMode() {
        Player p = getPlayer();
        return p == null ? GameMode.SURVIVAL : p.getGameMode();
    }

    // Separator

    public double getEyeHeight() {
        Player p = getPlayer();
        return p == null ? 0.0 : p.getEyeHeight();
    }

    // Separator

    public boolean isFrozen() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            Player p = getPlayer();
            return p != null && p.isFrozen();
        } else {
            return false;
        }
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

    // Separator

    public boolean hasAttackCooldown() {
        return getAttackCooldown() != 1.0f;
    }

    public float getAttackCooldown() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            Player p = getPlayer();
            return p == null ? 1.0f : p.getAttackCooldown();
        } else {
            return 1.0f;
        }
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
                        SpartanLocation loc = this.movement.getEventToLocation();
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
                SpartanLocation loc = this.movement.getEventToLocation();
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

    public void groundTeleport() {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection") || this.onGroundCustom) {
            return;
        }
        SpartanLocation location = this.movement.getLocation(),
                locationP1 = location.clone().add(0, 1, 0);

        if (BlockUtils.isSolid(locationP1.getBlock().material)
                && !(BlockUtils.areWalls(locationP1.getBlock().material)
                || BlockUtils.canClimb(locationP1.getBlock().material, false))) {
            return;
        }
        World world = getWorld();
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
                    && (BlockUtils.canClimb(material, false)
                    || BlockUtils.areWalls(material)
                    || !BlockUtils.isSolid(material))) {
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

        if (iterations > 0) {
            setFallDistance(0.0f);

            if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")
                    && iterations > PlayerUtils.fallDamageAboveBlocks
                    && playerProfile.isSuspectedOrHacker()) { // Damage
                applyFallDamage(Math.max(getFallDistance(), iterations));
            }
        }
    }

    public boolean teleport(SpartanLocation location) {
        if (this.getWorld().equals(location.world)) {
            Player p = getPlayer();

            if (p != null) {
                if (SpartanBukkit.isSynchronised()) {
                    p.leaveVehicle();
                }
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

        if (p != null) {
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
            Player p = getPlayer();

            if (p != null) {
                return p.canSee(target);
            }
        }
        return false;
    }

    public int getPing() {
        Player p = getPlayer();
        return p == null ? 0 : Latency.ping(p);
    }
}
