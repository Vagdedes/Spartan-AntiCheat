package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_World;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
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
    private long lastInteraction, afk;

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
                    protocol.spartan.movement.schedulerFrom = protocol.spartan.movement.getLocation();
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
        this.afk = -1L;
    }

    public void setLastInteraction() {
        this.checkForAFK();
        this.lastInteraction = System.currentTimeMillis();
    }

    private void checkForAFK() {
        if (System.currentTimeMillis() - protocol.spartan.lastInteraction >= 60_000L) {
            if (protocol.spartan.afk == -1L) {
                protocol.spartan.afk = protocol.spartan.lastInteraction;
            }
        } else if (protocol.spartan.afk != -1L) {
            protocol.getProfile().setAFKFor(
                    System.currentTimeMillis(),
                    System.currentTimeMillis() - protocol.spartan.afk,
                    true
            );
            protocol.spartan.afk = -1L;
        }
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
        return this.protocol.getProfile().getRunner(hackType);
    }

    public void resetCrucialData() {
        this.protocol.loaded = false;

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
                        || PluginUtils.exists("viaversion")
                        || Compatibility.CompatibilityType.PROTOCOL_SUPPORT.isFunctional() ? 256 :
                        100;
    }

    public void sendInventoryCloseMessage(String message) {
        if (message != null) {
            this.protocol.bukkit.sendMessage(message);
        }
        SpartanBukkit.transferTask(this, this.protocol.bukkit::closeInventory);
    }

    public void sendImportantMessage(String message) {
        this.protocol.bukkit.sendMessage("");
        this.sendInventoryCloseMessage(message);
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

    public boolean isOnGround(SpartanLocation location, boolean checkEntities) {
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
                    && GroundUtils.isOnGround(this.protocol, movement.getLocation(), false, true);
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
                    protocol.spartan,
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

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        if (MultiVersion.folia || this.protocol.packetsEnabled()) {
            Map<Long, List<Entity>> perChunk = Event_World.getEntities(this.getWorld());

            if (perChunk != null) {
                List<Entity> nearbyEntities = new ArrayList<>();
                SpartanLocation current = this.movement.getLocation();
                Collection<SpartanLocation> surrounding = current.getSurroundingLocations(x, y, z),
                        locations = new ArrayList<>(surrounding.size() + 1);
                locations.add(current);
                locations.addAll(surrounding);

                for (SpartanLocation location : locations) {
                    List<Entity> list = perChunk.get(Event_Chunks.hashCoordinates(
                            location.getChunkX(),
                            location.getChunkZ()
                    ));

                    if (list != null) {
                        for (Entity entity : list) {
                            Location entityLocation = ProtocolLib.getLocation(entity);

                            if (Math.abs(entityLocation.getX() - current.getX()) <= x
                                    && Math.abs(entityLocation.getY() - current.getY()) <= y
                                    && Math.abs(entityLocation.getZ() - current.getZ()) <= z) {
                                nearbyEntities.add(entity);
                            }
                        }
                    }
                }
                return nearbyEntities;
            } else {
                return new ArrayList<>(0);
            }
        } else {
            Thread thread = Thread.currentThread();
            List<Entity> nearbyEntities = new ArrayList<>();
            boolean[] booleans = new boolean[1];
            SpartanBukkit.transferTask(
                    this,
                    () -> {
                        nearbyEntities.addAll(this.protocol.bukkit.getNearbyEntities(x, y, z));
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

        if (BlockUtils.isSolid(locationP1.getBlock().getType())
                && !(BlockUtils.areWalls(locationP1.getBlock().getType())
                || BlockUtils.canClimb(locationP1.getBlock().getType(), false))) {
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
                    teleport(loopLocation);
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

    public boolean teleport(SpartanLocation location) {
        if (this.getWorld().equals(location.world)) {
            if (SpartanBukkit.isSynchronised()) {
                this.protocol.bukkit.leaveVehicle();
            }
            this.movement.removeLastLiquidTime();
            this.trackers.removeMany(PlayerTrackers.TrackerFamily.VELOCITY);

            if (MultiVersion.folia) {
                this.protocol.bukkit.teleportAsync(location.bukkit());
            } else {
                SpartanBukkit.transferTask(this, () -> this.protocol.bukkit.teleport(location.bukkit()));
            }
            return true;
        } else {
            return false;
        }
    }

    // Separator

    public void damage(double amount) {
        this.protocol.bukkit.damage(amount);
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

}
