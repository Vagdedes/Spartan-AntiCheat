package ai.idealistic.spartan.functionality.server;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.inventory.implementation.*;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.compatibility.Compatibility;
import ai.idealistic.spartan.compatibility.necessary.protocollib.ProtocolLib;
import ai.idealistic.spartan.functionality.concurrent.GeneralThread;
import ai.idealistic.spartan.listeners.NPCManager;
import ai.idealistic.spartan.utils.java.ReflectionUtils;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PluginBase {

    public static final GeneralThread.ThreadPool
            connectionThread = new GeneralThread.ThreadPool(TPS.tickTime),
            dataThread = new GeneralThread.ThreadPool(1L);

    public static final PlayerInfo playerInfo = new PlayerInfo();
    public static final DetailsMenu detailsMenu = new DetailsMenu();
    public static final EditionMenu editionMenu = new EditionMenu();
    public static final PreventionsMenu preventionsMenu = new PreventionsMenu();
    public static final PunishmentsMenu punishmentsMenu = new PunishmentsMenu();
    public static final SynMenu synMenu = new SynMenu();
    public static final DetectionsMenu detectionsMenu = new DetectionsMenu();
    public static final GeneralMenu generalMenu = new GeneralMenu();
    public static final StatisticsMenu statisticsMenu = new StatisticsMenu();
    public static final CommandsMenu commandsMenu = new CommandsMenu();
    public static final SubCommandsMenu subCommandsMenu = new SubCommandsMenu();
    public static final InventoryMenu[] menus = new InventoryMenu[]{
            playerInfo,
            detailsMenu,
            editionMenu,
            preventionsMenu,
            punishmentsMenu,
            synMenu,
            detectionsMenu,
            generalMenu,
            statisticsMenu,
            commandsMenu,
            subCommandsMenu
    };

    public static final int
            hashCodeMultiplier = 31,
            maxBytes = AlgebraUtils.integerRound(Runtime.getRuntime().maxMemory() * 0.05),
            maxSQLRows = maxBytes / 1024;
    private static final Map<UUID, PlayerProtocol> playerProtocol = new ConcurrentHashMap<>();
    public static final Class<?> craftPlayer = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
            ? null
            : ReflectionUtils.getClass(
            ReflectionUtils.class.getPackage().getName().substring(0, 19) // Package
                    + "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().substring(23) + ".entity.CraftPlayer" // Version
    );

    public static int getPlayerCount() {
        return playerProtocol.size();
    }

    public static boolean hasPlayerCount() {
        return !playerProtocol.isEmpty();
    }

    public static Collection<PlayerProtocol> getProtocols() {
        return playerProtocol.values();
    }

    public static Set<Map.Entry<UUID, PlayerProtocol>> getPlayerEntries() {
        return playerProtocol.entrySet();
    }

    // Separator

    public static boolean isSynchronised() {
        return Bukkit.isPrimaryThread()
                || !Register.isPluginEnabled();
    }

    // Separator

    public static Object getCraftPlayerMethod(Player p, String path) {
        if (craftPlayer != null) {
            try {
                Object handle = craftPlayer.getMethod("getHandle", new Class[0]).invoke(p, new Object[0]);
                return handle.getClass().getDeclaredField(path).get(handle);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    // Separator

    public static boolean packetsEnabled() {
        return Compatibility.CompatibilityType.PROTOCOL_LIB.isFunctional();
    }

    public static PlayerProtocol getProtocol(Player player, boolean updateLastInteraction) {
        if (ProtocolLib.isTemporary(player) || !player.isOnline()) { // if the player is "offline" 100% NPC / fake player
            return new PlayerProtocol(player);
        } else {
            PlayerProtocol protocol = playerProtocol.get(player.getUniqueId());

            if (protocol == null) {
                protocol = new PlayerProtocol(player);
                PlayerProtocol existing = playerProtocol.putIfAbsent(player.getUniqueId(), protocol);

                if (existing != null) { // Another thread registered the player first
                    protocol = existing;
                    protocol.updateBukkit(player);
                }
            } else {
                protocol.updateBukkit(player);
            }
            if (updateLastInteraction) {
                protocol.setLastInteraction();
            }
            return protocol;
        }
    }

    public static PlayerProtocol getProtocol(Player player) {
        return getProtocol(player, false);
    }

    public static PlayerProtocol getProtocol(String name) {
        if (!playerProtocol.isEmpty()) {
            for (PlayerProtocol protocol : playerProtocol.values()) {
                if (protocol.bukkit().getName().equals(name)) {
                    return protocol;
                }
            }
        }
        return null;
    }

    public static PlayerProtocol getAnyCaseProtocol(String name) {
        if (!playerProtocol.isEmpty()) {
            for (PlayerProtocol protocol : playerProtocol.values()) {
                if (protocol.bukkit().getName().equalsIgnoreCase(name)) {
                    return protocol;
                }
            }
        }
        return null;
    }

    public static PlayerProtocol getProtocol(int entityID) {
        for (PlayerProtocol protocol : playerProtocol.values()) {
            if (protocol.getEntityId() == entityID) {
                return protocol;
            }
        }
        return null;
    }

    public static PlayerProtocol getProtocol(UUID uuid) {
        if (isSynchronised()) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                return getProtocol(player);
            } else {
                return playerProtocol.get(uuid);
            }
        } else {
            return playerProtocol.get(uuid);
        }
    }

    public static PlayerProtocol deleteProtocol(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return null;
        } else {
            PlayerProtocol protocol = playerProtocol.remove(player.getUniqueId());

            if (protocol != null && !protocol.isAFK()) {
                protocol.profile().getContinuity().setActiveTime(
                        System.currentTimeMillis(),
                        protocol.getActiveTimePlayed(),
                        true
                );
            }
            return protocol;
        }
    }

    // Separator

    public static Object runDelayedTask(PlayerProtocol protocol, Runnable runnable, long start) {
        return ServerSchedulers.schedule(protocol, runnable, start, -1L);
    }

    public static Object runRepeatingTask(PlayerProtocol protocol, Runnable runnable, long start, long repetition) {
        return ServerSchedulers.schedule(protocol, runnable, start, repetition);
    }

    public static Object runDelayedTask(Runnable runnable, long start) {
        return ServerSchedulers.schedule(null, runnable, start, -1L);
    }

    public static Object runRepeatingTask(Runnable runnable, long start, long repetition) {
        return ServerSchedulers.schedule(null, runnable, start, repetition);
    }

    public static void runTask(PlayerProtocol protocol, Runnable runnable) {
        ServerSchedulers.run(protocol, runnable, false);
    }

    public static void runTask(World world, int x, int z, Runnable runnable) {
        ServerSchedulers.run(world, x, z, runnable, false);
    }

    public static void runTask(ServerLocation location, Runnable runnable) {
        ServerSchedulers.run(location.getWorld(), location.getChunkX(), location.getChunkZ(), runnable, false);
    }

    public static void runTask(Block block, Runnable runnable) {
        ServerSchedulers.run(block.getWorld(), block.getX() >> 4, block.getZ() >> 4, runnable, false);
    }

    public static void runTask(Entity entity, Runnable runnable) {
        ServerSchedulers.run(entity.getWorld(), entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4, runnable, false);
    }

    public static void transferTask(PlayerProtocol protocol, Runnable runnable) {
        ServerSchedulers.run(protocol, runnable, true);
    }

    public static void transferTask(World world, int x, int z, Runnable runnable) {
        ServerSchedulers.run(world, x, z, runnable, true);
    }

    public static void transferTask(Block block, Runnable runnable) {
        ServerSchedulers.run(block.getWorld(), block.getX() >> 4, block.getZ() >> 4, runnable, true);
    }

    public static void transferTask(Entity entity, Runnable runnable) {
        ServerSchedulers.run(entity.getWorld(), entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4, runnable, true);
    }

    public static void transferTask(Runnable runnable) {
        ServerSchedulers.transfer(runnable);
    }

    public static void cancelTask(Object task) {
        ServerSchedulers.cancel(task);
    }

    // Separator

    public static void runCommand(String command) {
        PluginBase.transferTask(
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        );
    }

    // Separator

    public static void disable() {
        if (!playerProtocol.isEmpty()) {
            for (PlayerProtocol protocol : playerProtocol.values()) {
                if (protocol != null && !protocol.isAFK()) {
                    protocol.profile().getContinuity().setActiveTime(
                            System.currentTimeMillis(),
                            protocol.getActiveTimePlayed(),
                            true
                    );
                }
            }
        }

        playerProtocol.clear();
        GeneralThread.disable();
        NPCManager.clear();
        Config.create();
    }
}