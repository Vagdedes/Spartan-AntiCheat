package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Clicks;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
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
import com.vagdedes.spartan.listeners.bukkit.Event_Movement;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
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

    public final SpartanProtocol protocol;
    public final boolean bedrockPlayer;
    public final String name;
    public final UUID uuid;
    private final Map<PotionEffectType, SpartanPotionEffect> potionEffects;
    public final Enums.DataType dataType;
    public final SpartanPlayerMovement movement;
    public final SpartanPunishments punishments;
    public final Buffer buffer;
    public final Cooldowns cooldowns;
    public final Trackers trackers;
    public final Clicks clicks;

    private final long tick;
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
        SpartanBukkit.runRepeatingTask(() -> {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (SpartanPlayer p : players) {
                    p.movement.judgeGround();

                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                        Elytra.judge(p, false);
                    }
                    if (p.movement.isFlying()) {
                        p.trackers.add(Trackers.TrackerType.FLYING, (int) TPS.maximum);
                    }
                    p.playerProfile.playerCombat.track();

                    // Separator
                    SpartanBukkit.runTask(p, () -> {
                        p.setStoredPotionEffects(p.getInstance().getActivePotionEffects()); // Bad
                        SpartanLocation to = p.movement.getLocation(),
                                from = p.movement.getSchedulerFromLocation();

                        if (from != null) {
                            p.movement.schedulerDistance = to.distance(from);
                        }
                        p.movement.schedulerFrom = to;

                        for (CheckExecutor executor : p.executors) {
                            executor.scheduler();
                        }

                        // Preventions
                        for (Enums.HackType hackType : Event_Movement.handledChecks) {
                            if (p.getViolations(hackType).prevent()) {
                                break;
                            }
                        }
                    });
                }
            }
        }, 1L, 1L);
    }

    // Object

    public SpartanPlayer(SpartanProtocol protocol) {
        Player p = protocol.player;
        Enums.HackType[] hackTypes = Enums.HackType.values();

        this.protocol = protocol;
        this.uuid = p.getUniqueId();
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? Enums.DataType.BEDROCK : Enums.DataType.JAVA;
        this.trackers = new Trackers(this);

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
        this.lastDamageReceived = new SpartanPlayerDamage(this);
        this.lastDamageDealt = new SpartanPlayerDamage(this);

        // Load them all to keep their order
        for (EntityDamageEvent.DamageCause damageCause : EntityDamageEvent.DamageCause.values()) {
            this.damageReceived.put(
                    damageCause,
                    new SpartanPlayerDamage(this)
            );
            this.damageDealt.put(
                    damageCause,
                    new SpartanPlayerDamage(this)
            );
        }
        this.tick = System.currentTimeMillis();

        this.buffer = new Buffer(this);
        this.cooldowns = new Cooldowns(this);
        this.executors = new CheckExecutor[hackTypes.length];
        this.violations = new LiveViolation[hackTypes.length];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            this.violations[id] = new LiveViolation(this, hackType);

            try {
                CheckExecutor executor = (CheckExecutor) hackType.executor
                        .getConstructor(hackType.getClass(), this.getClass())
                        .newInstance(hackType, this);
                this.executors[id] = executor;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean debug(boolean function, boolean broadcast, boolean cutDecimals, Object... message) {
        if (function && SpartanBukkit.testMode) {
            Player p = getInstance();

            if (p.isWhitelisted()) {
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
                            if (o != null && o.getInstance().isOp()) {
                                o.getInstance().sendMessage(string);
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
            this.getExecutor(Enums.HackType.FastClicks).run(false);
            InteractiveInventory.playerInfo.refresh(name);
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
        this.getInstance().sendMessage(message);
    }

    public void sendInventoryCloseMessage(String message) {
        if (message != null) {
            this.getInstance().sendMessage(message);
        }
        SpartanBukkit.transferTask(this, this.getInstance()::closeInventory);
    }

    public void sendImportantMessage(String message) {
        this.getInstance().sendMessage("");
        this.sendInventoryCloseMessage(message);
        this.getInstance().sendMessage("");
    }

    // Separator

    public Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(this.getInstance(), size, title);
    }

    public InventoryView getOpenInventory() {
        return this.getInstance().getOpenInventory();
    }

    // Separator

    public PlayerInventory getInventory() {
        return this.getInstance().getInventory();
    }

    public ItemStack getItemInHand() {
        return this.getInventory().getItemInHand();
    }

    // Separator

    public boolean isOnFire() {
        return !this.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE, 0)
                && (this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE).ticksPassed() <= 5
                || this.getDamageReceived(EntityDamageEvent.DamageCause.FIRE_TICK).ticksPassed() <= 5);
    }

    // Separator

    public Player getInstance() {
        return this.protocol.player;
    }

    public long ticksPassed() {
        return TPS.getTick(this) - this.tick;
    }

    // Separator

    public boolean isOnGround(SpartanLocation location, boolean checkEntities) {
        return GroundUtils.isOnGround(
                this,
                location,
                this.getInstance().isOnGround()
                        || this.protocol.isOnGround(),
                checkEntities
        );
    }

    public boolean isOnGround(boolean custom) {
        Entity vehicle = this.getInstance().getVehicle();

        if (vehicle != null) {
            return vehicle.isOnGround();
        } else if (SpartanBukkit.packetsEnabled()) {
            return this.protocol.isOnGround()
                    || custom
                    && GroundUtils.isOnGround(this, movement.getLocation(), false, true);
        } else {
            return this.getInstance().isOnGround()
                    || custom
                    && GroundUtils.isOnGround(this, movement.getLocation(), false, true);
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
                List<Block> list = this.getInstance().getLineOfSight(null, (int) distance);

                if (!list.isEmpty()) {
                    for (Block block : list) {
                        if (BlockUtils.isFullSolid(block.getType())) {
                            return new SpartanBlock(block);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // Separator

    public String getIpAddress() {
        return PlayerLimitPerIP.get(this.getInstance());
    }

    public boolean isFrozen() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.getInstance().isFrozen();
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
        this.lastDamageReceived = new SpartanPlayerDamage(this, event);

        synchronized (this.damageReceived) {
            this.damageReceived.put(
                    event.getCause(),
                    this.lastDamageReceived
            );
        }
    }

    public void addDealtDamage(EntityDamageByEntityEvent event) {
        this.lastDamageDealt = new SpartanPlayerDamage(this, event);

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
            return this.getInstance().getAttackCooldown();
        } else {
            return 1.0f;
        }
    }

    // Separator

    public boolean hasActivePotionEffects() {
        Entity vehicle = this.getInstance().getVehicle();

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
        Entity vehicle = this.getInstance().getVehicle();

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
        Thread thread = Thread.currentThread();
        List<Entity> nearbyEntities = new ArrayList<>();
        boolean[] booleans = new boolean[1];
        SpartanBukkit.transferTask(
                this,
                () -> {
                    nearbyEntities.addAll(this.getInstance().getNearbyEntities(x, y, z));
                    booleans[0] = true;

                    synchronized (thread) {
                        thread.notifyAll();
                    }
                }
        );
        synchronized (thread) {
            if (!booleans[0]) {
                try {
                    thread.wait();
                } catch (Exception ignored) {
                }
            }
        }
        return nearbyEntities;
    }

    // Teleport

    public void groundTeleport() {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection")) {
            return;
        }
        SpartanLocation location = this.movement.getLocation();

        if (this.isOnGround(location, true)) {
            return;
        }
        SpartanLocation locationP1 = location.clone().add(0, 1, 0);

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
            this.getInstance().setFallDistance(0.0f);

            if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")
                    && iterations > PlayerUtils.fallDamageAboveBlocks) { // Damage
                damage(Math.max(this.getInstance().getFallDistance(), iterations));
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

    public boolean damage(double amount) {
        Player p = getInstance();

        if (p != null) {
            trackers.disable(Trackers.TrackerType.ABSTRACT_VELOCITY, 3);
            p.damage(amount);
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

    public int getPing() {
        return Math.max(
                Latency.ping(this.getInstance()),
                this.protocol.getPing()
        );
    }
}
