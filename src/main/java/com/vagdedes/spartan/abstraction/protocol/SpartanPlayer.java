package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_Chunks;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpartanPlayer {

    final SpartanProtocol protocol;
    private final Map<PotionEffectType, ExtendedPotionEffect> potionEffects;
    public final Check.DataType dataType;
    public final PlayerMovement movement;
    public final PlayerPunishments punishments;
    public final PlayerTrackers trackers;
    public final PlayerClicks clicks;
    private long lastInteraction;
    private boolean afk;

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

            if (!protocols.isEmpty()) {
                for (SpartanProtocol protocol : protocols) {
                    if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                        protocol.spartan.setStoredPotionEffects();
                    }
                    protocol.spartan.getRunner(Enums.HackType.AutoRespawn).run(false);
                    protocol.spartan.getRunner(Enums.HackType.MorePackets).run(false);
                    protocol.spartan.getRunner(Enums.HackType.Exploits).run(false);
                    protocol.spartan.movement.schedulerFrom = protocol.getLocation();
                    protocol.spartan.checkForAFK();
                }
            }
        }, 1L, 1L);
    }

    // Object

    public SpartanPlayer(SpartanProtocol protocol) {
        this.protocol = protocol;
        this.dataType = BedrockCompatibility.isPlayer(protocol.bukkit)
                ? Check.DataType.BEDROCK
                : Check.DataType.JAVA;
        this.trackers = new PlayerTrackers();

        this.potionEffects = new ConcurrentHashMap<>(2);
        this.setStoredPotionEffects();

        this.clicks = new PlayerClicks();
        this.movement = new PlayerMovement(this);
        this.punishments = new PlayerPunishments(this);

        this.lastInteraction = System.currentTimeMillis();
        this.afk = false;
    }

    public void setLastInteraction() {
        this.lastInteraction = System.currentTimeMillis();
    }

    private void checkForAFK() {
        long lastInteraction = System.currentTimeMillis() - this.lastInteraction;

        if (lastInteraction >= 30_000L) {
            if (!afk) {
                this.protocol.profile().getContinuity().setActiveTime(
                        System.currentTimeMillis(),
                        this.protocol.getActiveTimePlayed() - lastInteraction,
                        true
                );
                this.afk = true;
            }
        } else if (this.afk) {
            this.protocol.resetActiveCreationTime();
            this.afk = false;
        }
    }

    public boolean isAFK() {
        return this.afk;
    }

    public boolean isBedrockPlayer() {
        return this.dataType == Check.DataType.BEDROCK;
    }

    @Override
    public String toString() {
        return this.protocol.bukkit.getName();
    }

    public boolean debug(boolean function, boolean broadcast, boolean cutDecimals, Object... message) {
        if (function && Bukkit.getMotd().contains(Register.plugin.getName())) {
            if (this.protocol.bukkit.isWhitelisted()) {
                String string = "§f" + this.protocol.bukkit.getName() + " ";

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
                List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

                if (!protocols.isEmpty()) {
                    if (broadcast) {
                        Bukkit.broadcastMessage(string);
                    } else {
                        for (SpartanProtocol protocol : protocols) {
                            if (protocol.bukkit.isOp()) {
                                protocol.bukkit.sendMessage(string);
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

    public CheckRunner getRunner(Enums.HackType hackType) {
        return this.protocol.profile().getRunner(hackType);
    }

    public void resetCrucialData() {
        for (PlayerTrackers.TrackerType handlerType : PlayerTrackers.TrackerType.values()) {
            this.trackers.remove(handlerType);
        }
        if (!this.movement.judgeGround()) {
            this.movement.resetAirTicks();
        }
    }

    // Separator

    public void calculateClicks(boolean run) {
        if (run) {
            clicks.calculate();
            this.getRunner(Enums.HackType.FastClicks).run(false);
            InteractiveInventory.playerInfo.refresh(this.protocol.bukkit.getName());
        }
    }

    // Separator

    public int getMaxChatLength() {
        return this.isBedrockPlayer() ? 512 :
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                        || this.protocol.isUsingVersionOrGreater(MultiVersion.MCVersion.V1_11)
                        || PluginUtils.exists("viaversion")
                        || Compatibility.CompatibilityType.PROTOCOL_SUPPORT.isFunctional() ? 256 :
                        100;
    }

    public void sendImportantMessage(String message) {
        this.protocol.bukkit.sendMessage("");
        this.protocol.bukkit.sendMessage(message);
        this.protocol.bukkit.sendMessage("");
    }

    // Separator

    public Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(this.protocol.bukkit, size, title);
    }

    // Separator

    public ItemStack getItemInHand() {
        return this.protocol.bukkit.getInventory().getItemInHand();
    }

    // Separator

    public boolean isOnGround(Location location, boolean checkEntities) {
        return GroundUtils.isOnGround(
                this.protocol,
                location,
                this.protocol.isOnGround(),
                checkEntities
        );
    }

    public boolean isOnGround(boolean custom) {
        Entity vehicle = this.protocol.spartan.getVehicle();

        if (vehicle != null) {
            return vehicle.isOnGround();
        } else {
            return this.protocol.isOnGround()
                    || custom
                    && GroundUtils.isOnGround(this.protocol, protocol.getLocation(), false, true);
        }
    }

    // Separator

    public Block getTargetBlock(double distance, double limit) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            try {
                List<Block> list = this.protocol.bukkit.getLineOfSight(null, (int) Math.min(distance, limit));

                if (!list.isEmpty()) {
                    for (Block block : list) {
                        if (BlockUtils.isFullSolid(block.getType())) {
                            return block;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // Separator

    public boolean isFrozen() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.protocol.bukkit.isFrozen();
        } else {
            return false;
        }
    }

    // Separator

    public void handleReceivedDamage() {
        this.trackers.add(PlayerTrackers.TrackerType.DAMAGE, AlgebraUtils.integerCeil(TPS.maximum));
    }

    // Separator

    public float getAttackCooldown() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.protocol.bukkit.getAttackCooldown();
        } else {
            return 1.0f;
        }
    }

    // Separator

    public Entity getVehicle() {
        return ProtocolLib.getVehicle(this.protocol.bukkit);
    }

    public double getHealth() {
        return ProtocolLib.isTemporary(this.protocol.bukkit)
                ? 0.0
                : this.protocol.bukkit.getHealth();
    }

    public int getEntityId() {
        return ProtocolLib.isTemporary(this.protocol.bukkit)
                ? new Random().nextInt()
                : this.protocol.bukkit.getEntityId();
    }

    // Separator

    private void setStoredPotionEffects() {
        if (!ProtocolLib.isTemporary(this.protocol.bukkit)) {
            SpartanBukkit.transferTask(
                    protocol,
                    () -> {
                        for (PotionEffect effect : this.protocol.bukkit.getActivePotionEffects()) {
                            this.potionEffects.put(effect.getType(), new ExtendedPotionEffect(effect));
                        }
                    }
            );
        }
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        return ProtocolLib.isTemporary(this.protocol.bukkit)
                ? new ArrayList<>(0)
                : this.protocol.bukkit.getActivePotionEffects();
    }

    public ExtendedPotionEffect getPotionEffect(PotionEffectType type, long lastActive) {
        this.setStoredPotionEffects();
        ExtendedPotionEffect potionEffect = this.potionEffects.get(type);

        if (potionEffect != null
                && potionEffect.timePassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type)) {
            return potionEffect;
        } else {
            return null;
        }
    }

    public boolean hasPotionEffect(PotionEffectType type, long lastActive) {
        this.setStoredPotionEffects();
        ExtendedPotionEffect potionEffect = this.potionEffects.get(type);
        return potionEffect != null
                && potionEffect.timePassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type);
    }

    // Separator

    public List<Entity> getNearbyEntities(double radius) {
        if (SpartanBukkit.isSynchronised()) {
            return this.protocol.bukkit.getNearbyEntities(radius, radius, radius);
        } else {
            List<Entity> entities = new ArrayList<>();
            Location location = protocol.getLocation();
            World world = location.getWorld();
            int smallX = MathHelper.floor_double((location.getX() - radius) / PlayerUtils.chunk);
            int bigX = MathHelper.floor_double((location.getX() + radius) / PlayerUtils.chunk);
            int smallZ = MathHelper.floor_double((location.getZ() - radius) / PlayerUtils.chunk);
            int bigZ = MathHelper.floor_double((location.getZ() + radius) / PlayerUtils.chunk);
            double radiusSquared = radius * radius;

            for (int xx = smallX; xx <= bigX; xx++) {
                for (int zz = smallZ; zz <= bigZ; zz++) {
                    if (Event_Chunks.isLoaded(world, xx, zz)) {
                        for (Entity entity : world.getChunkAt(xx, zz).getEntities()) {
                            if (SpartanLocation.distanceSquared(
                                    entity.getLocation(),
                                    location
                            ) <= radiusSquared) {
                                entities.add(entity);
                            }
                        }
                    }
                }
            }
            return entities;
        }
    }

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        if (SpartanBukkit.isSynchronised()) {
            return this.protocol.bukkit.getNearbyEntities(x, y, z);
        } else {
            List<Entity> entities = new ArrayList<>();
            Location location = protocol.getLocation();
            World world = location.getWorld();
            int smallX = MathHelper.floor_double((location.getX() - x) / PlayerUtils.chunk);
            int bigX = MathHelper.floor_double((location.getX() + x) / PlayerUtils.chunk);
            int smallZ = MathHelper.floor_double((location.getZ() - z) / PlayerUtils.chunk);
            int bigZ = MathHelper.floor_double((location.getZ() + z) / PlayerUtils.chunk);

            for (int xx = smallX; xx <= bigX; xx++) {
                for (int zz = smallZ; zz <= bigZ; zz++) {
                    if (Event_Chunks.isLoaded(world, xx, zz)) {
                        for (Entity entity : world.getChunkAt(xx, zz).getEntities()) {
                            Location eLoc = entity.getLocation();

                            if (Math.abs(eLoc.getX() - location.getX()) <= x
                                    && Math.abs(eLoc.getY() - location.getY()) <= y
                                    && Math.abs(eLoc.getZ() - location.getZ()) <= z) {
                                entities.add(entity);
                            }
                        }
                    }
                }
            }
            return entities;
        }
    }

    // Teleport

    public void groundTeleport() {
        if (!Config.settings.getBoolean("Detections.ground_teleport_on_detection")) {
            return;
        }
        Location location = this.protocol.getLocation();

        if (this.isOnGround(location, true)) {
            return;
        }
        SpartanLocation locationP1 = new SpartanLocation(location.clone().add(0, 1, 0));

        if (BlockUtils.isSolid(locationP1.getBlock().getType())
                && !(BlockUtils.areWalls(locationP1.getBlock().getType())
                || BlockUtils.canClimb(locationP1.getBlock().getType(), false))) {
            return;
        }
        World world = this.protocol.getWorld();
        double startY = Math.min(BlockUtils.getMaxHeight(world), location.getY()),
                box = startY - Math.floor(startY);
        int iterations = 0;

        if (!GroundUtils.blockHeightExists(box)) {
            box = 0.0;
        }

        for (double progressiveY = startY; startY > BlockUtils.getMinHeight(world); progressiveY--) {
            SpartanLocation loopLocation = new SpartanLocation(
                    location.clone().add(0.0, -(startY - progressiveY), 0.0)
            );
            Material material = loopLocation.getBlock().getType();

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
                if (this.protocol.packetsEnabled()) {
                    this.protocol.teleport(loopLocation.bukkit());
                }
                break;
            }
        }

        if (iterations > 0) {
            this.protocol.bukkit.setFallDistance(0.0f);

            if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")
                    && iterations > PlayerUtils.fallDamageAboveBlocks) { // Damage
                damage(Math.max(this.protocol.bukkit.getFallDistance(), iterations));
            }
        }
    }

    // Separator

    public void damage(double amount) {
        this.protocol.bukkit.damage(amount);
    }

    // Separator

    public boolean isOutsideOfTheBorder(double deviation) {
        Location loc = protocol.getLocation();
        WorldBorder border = this.protocol.getWorld().getWorldBorder();
        double size = (border.getSize() / 2.0) + deviation;
        Location center = border.getCenter();
        return Math.abs(loc.getX() - center.getX()) > size
                || Math.abs(loc.getZ() - center.getZ()) > size;
    }

}
