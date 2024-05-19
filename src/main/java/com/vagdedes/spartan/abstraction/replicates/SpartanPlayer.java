package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.CheckExecutorExample;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Clicks;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
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
import com.vagdedes.spartan.listeners.protocol.ProtocolStorage;
import com.vagdedes.spartan.listeners.protocol.Shared;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.*;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

    public final boolean bedrockPlayer;
    public final String name;
    public final UUID uuid;
    public final String ipAddress;
    private final Map<PotionEffectType, SpartanPotionEffect> potionEffects;
    public final Enums.DataType dataType;
    public final SpartanPlayerMovement movement;
    public final SpartanPunishments punishments;
    public final Buffer buffer;
    public final Cooldowns cooldowns;
    public final Trackers trackers;
    public final Clicks clicks;

    private final long creationTime;
    private final Map<EntityDamageEvent.DamageCause, SpartanPlayerDamage>
            damageReceived,
            damageDealt;
    private final List<SpartanPlayerVelocity> velocityReceived;
    private final CheckExecutor[] executors;
    private final LiveViolation[] violations;

    private SpartanPlayerDamage
            lastDamageReceived,
            lastDamageDealt;
    private PlayerProfile playerProfile;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    for (SpartanPlayer p : players) {
                        p.movement.judgeGround();
                        p.movement.getLocation();

                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                            Elytra.judge(p, false);
                        }
                        p.playerProfile.playerCombat.track();
                        p.movement.setDetectionLocation(false);

                        // Separator
                        SpartanBukkit.runTask(p, () -> {
                            Player n = p.getInstance();

                            if (n != null) {
                                p.setStoredPotionEffects(n.getActivePotionEffects()); // Bad
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
                            for (Enums.HackType hackType : Shared.handledChecks) {
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

    // Object

    public SpartanPlayer(Player p, UUID uuid) {
        Enums.HackType[] hackTypes = Enums.HackType.values();

        this.uuid = uuid;
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

        this.buffer = new Buffer(this);
        this.cooldowns = new Cooldowns(this);
        this.executors = new CheckExecutor[hackTypes.length];
        this.violations = new LiveViolation[hackTypes.length];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            this.violations[id] = new LiveViolation(this, hackType);
            this.executors[id] = new CheckExecutorExample(this);
        }
        if (CheckExecutorExample.executors.length > 0) {
            for (Class<?> executorClass : CheckExecutorExample.executors) {
                try {
                    CheckExecutor executor = (CheckExecutor) executorClass.getConstructor(SpartanPlayer.class).newInstance(this);
                    this.executors[executor.hackType.ordinal()] = executor;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public boolean debug(boolean function, boolean broadcast, boolean cutDecimals, Object... message) {
        if (function && SpartanBukkit.testMode) {
            Player p = getInstance();

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
                                Player no = o.getInstance();

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

    public CheckExecutor getExecutor(Enums.HackType hackType) {
        return executors[hackType.ordinal()];
    }

    public void resetTrackers() {
        for (Trackers.TrackerType handlerType : Trackers.TrackerType.values()) {
            trackers.remove(handlerType);
        }
    }

    // Separator

    public LiveViolation getViolations(Enums.HackType hackType) {
        return violations[hackType.ordinal()];
    }

    // Separator

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
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                        || PluginUtils.exists("viaversion")
                        || Compatibility.CompatibilityType.PROTOCOL_SUPPORT.isFunctional() ? 256 :
                        100;
    }

    public void sendMessage(String message) {
        Player p = getInstance();

        if (p != null) {
            p.sendMessage(message);
        }
    }

    public void sendInventoryCloseMessage(String message) {
        Player p = getInstance();

        if (p != null) {
            if (message != null) {
                p.sendMessage(message);
            }
            p.closeInventory();
        }
    }

    public void sendImportantMessage(String message) {
        Player p = getInstance();

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
            Player p = getInstance();

            if (p != null) {
                p.openInventory(inventory);
            }
        }
    }

    public Inventory createInventory(int size, String title) {
        Player p = getInstance();

        if (p != null) {
            return Bukkit.createInventory(p, size, title);
        }
        return null;
    }

    public InventoryView getOpenInventory() {
        Player p = getInstance();
        return p == null ? null : p.getOpenInventory();
    }

    // Separator

    public PlayerInventory getInventory() {
        Player p = getInstance();
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

    public Player getInstance() {
        return SpartanBukkit.getRealPlayer(uuid);
    }

    public long timePassedSinceCreation() {
        return System.currentTimeMillis() - creationTime;
    }

    // Separator

    public boolean isOnGround(SpartanLocation location) {
        Player p = this.getInstance();
        return GroundUtils.isOnGround(
                this,
                location,
                p != null && p.isOnGround()
        );
    }

    public boolean isOnGround() {
        Entity vehicle = getVehicle();

        if (vehicle != null) {
            return vehicle.isOnGround();
        } else if (SpartanBukkit.packetsEnabled()) {
            return ProtocolStorage.isOnGround(this)
                    || bedrockPlayer
                    && GroundUtils.isOnGround(this, movement.getLocation(), false);
        } else {
            Player p = this.getInstance();
            return p != null && p.isOnGround()
                    || bedrockPlayer
                    && GroundUtils.isOnGround(this, movement.getLocation(), false);
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
                Player n = getInstance();

                if (n != null) {
                    List<Block> list = n.getLineOfSight(null, (int) distance);

                    if (!list.isEmpty()) {
                        for (Block block : list) {
                            if (BlockUtils.isFullSolid(block.getType())) {
                                return new SpartanBlock(block);
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

    public boolean isOp() {
        Player p = this.getInstance();
        return p != null && p.isOp();
    }

    public boolean isDead() {
        Player p = getInstance();
        return p != null && p.isDead();
    }

    public boolean isSleeping() {
        Player p = getInstance();
        return p != null && p.isSleeping();
    }

    public float getWalkSpeed() {
        Player p = getInstance();
        return p == null ? 0.0f : p.getWalkSpeed();
    }

    public float getFlySpeed() {
        Player p = getInstance();
        return p == null ? 0.0f : p.getFlySpeed();
    }

    public Entity getVehicle() {
        Player p = getInstance();
        return p == null ? null : p.getVehicle();
    }

    public int getFoodLevel() {
        Player p = getInstance();
        return p == null ? 0 : p.getFoodLevel();
    }

    public double getHealth() {
        Player p = getInstance();
        return p == null ? 0.0 : p.getHealth();
    }

    public float getFallDistance() {
        Player p = getInstance();
        return p == null ? 0.0f : p.getFallDistance();
    }

    public GameMode getGameMode() {
        Player p = getInstance();
        return p == null ? GameMode.SURVIVAL : p.getGameMode();
    }

    public double getEyeHeight() {
        Player p = getInstance();
        return p == null ? 0.0 : p.getEyeHeight();
    }

    public boolean isFrozen() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            Player p = getInstance();
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
            Player p = getInstance();
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
        Player p = this.getInstance();

        if (p != null) {
            List<Entity> nearbyEntities = new ArrayList<>();
            SpartanBukkit.transferTask(
                    this,
                    () -> nearbyEntities.addAll(p.getNearbyEntities(x, y, z))
            );
            return nearbyEntities;
        } else {
            return new ArrayList<>(0);
        }
    }

    // Teleport

    public void groundTeleport() {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection")
                || this.isOnGround()) {
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
            Player p = getInstance();

            if (p != null) {
                p.setFallDistance(0.0f);
            }
            if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")
                    && iterations > PlayerUtils.fallDamageAboveBlocks
                    && playerProfile.isSuspectedOrHacker()) { // Damage
                applyFallDamage(Math.max(getFallDistance(), iterations));
            }
        }
    }

    public boolean teleport(SpartanLocation location) {
        if (this.getWorld().equals(location.world)) {
            Player p = getInstance();

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
        Player p = getInstance();

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
            Player p = getInstance();
            return p != null && p.canSee(target);
        }
        return false;
    }

    public int getPing() {
        Player p = getInstance();
        return p == null ? 0 : Latency.ping(p);
    }
}
