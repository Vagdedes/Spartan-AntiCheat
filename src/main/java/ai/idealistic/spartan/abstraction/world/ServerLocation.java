package ai.idealistic.spartan.abstraction.world;

import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.listeners.bukkit.standalone.ChunksEvent;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

@Data
public class ServerLocation implements Cloneable {

    public static Material emptyMaterial = Material.STONE;
    public static final Location bukkitDefault = new Location(
            Bukkit.getWorlds().get(0),
            AlgebraUtils.randomInteger(Integer.MIN_VALUE, Integer.MAX_VALUE),
            AlgebraUtils.randomInteger(PlayerUtils.minHeight, PlayerUtils.maxHeight),
            AlgebraUtils.randomInteger(Integer.MIN_VALUE, Integer.MAX_VALUE)
    );

    public static int getChunkPos(int pos) {
        return pos >> 4;
    }

    public static Location getBlockLocation(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public static double distance(Location loc1, Location loc2) {
        return AlgebraUtils.getDistance(
                loc1.getX(),
                loc2.getX(),
                loc1.getY(),
                loc2.getY(),
                loc1.getZ(),
                loc2.getZ()
        );
    }

    public static double distanceSquared(Location loc1, Location loc2) {
        return AlgebraUtils.getSquaredDistance(
                loc1.getX(),
                loc2.getX(),
                loc1.getY(),
                loc2.getY(),
                loc1.getZ(),
                loc2.getZ()
        );
    }

    private static int locationIdentifier(int x, int y, int z) {
        x = (PluginBase.hashCodeMultiplier * x) + y;
        return (PluginBase.hashCodeMultiplier * x) + z;
    }

    // Object

    public final World world;
    private double x, y, z;
    private float yaw, pitch;

    // Base

    public ServerLocation(World world,
                          double x, double y, double z,
                          float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public ServerLocation(World world, double x, double y, double z) {
        this(world, x, y, z, 0.0f, 0.0f);
    }

    public ServerLocation(Location loc) {
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    // Methods

    public ServerLocation clone() {
        try {
            return (ServerLocation) super.clone();
        } catch (Exception ex) {
            return null;
        }
    }

    public int getBlockX() {
        return AlgebraUtils.integerFloor(this.x);
    }

    public int getBlockY() {
        return AlgebraUtils.integerFloor(this.y);
    }

    public int getBlockZ() {
        return AlgebraUtils.integerFloor(this.z);
    }

    public int getChunkX() {
        return getChunkPos(getBlockX());
    }

    public int getChunkZ() {
        return getChunkPos(getBlockZ());
    }

    public int getLocalX() {
        return getBlockX() & 15;
    }

    public int getLocalY() {
        if (ChunksEvent.heightSupport) {
            return Math.max(this.world.getMinHeight(), Math.min(getBlockY(), this.world.getMaxHeight()));
        } else {
            return Math.max(0, Math.min(getBlockY(), PlayerUtils.maxHeight));
        }
    }

    public int getLocalZ() {
        return getBlockZ() & 15;
    }

    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }

    public ServerLocation subtract(double x, double y, double z) {
        return add(-x, -y, -z);
    }

    public ServerLocation subtract(ServerLocation loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public ServerLocation subtract(Location loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public ServerLocation subtract(Vector vec) {
        return subtract(vec.getX(), vec.getY(), vec.getZ());
    }

    public ServerLocation add(Vector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public ServerLocation add(ServerLocation loc) {
        return add(loc.getX(), loc.getY(), loc.getZ());
    }

    public ServerLocation add(double x, double y, double z) {
        Location bukkit = new Location(this.world, this.x, this.y, this.z).add(x, y, z);
        this.x = bukkit.getX();
        this.y = bukkit.getY();
        this.z = bukkit.getZ();
        return this;
    }

    private ServerBlock setBlock() {
        return new ServerBlock(this.world.getBlockAt(getBlockX(), getBlockY(), getBlockZ()));
    }

    private ServerBlock setAsyncBlock(PlayerProtocol protocol) {
        if (protocol == null) {
            return new ServerBlock(
                    ChunksEvent.getBlockAsync(this.bukkit())
            );
        } else {
            return new ServerBlock(
                    protocol.packetWorld.getBlock(this.bukkit())
            );
        }
    }

    public ServerBlock getBlock() {
        return getBlock(null);
    }

    public ServerBlock getBlock(PlayerProtocol protocol) {
        int blockY = getBlockY();

        if (ChunksEvent.heightSupport ?
                blockY >= this.world.getMinHeight() && blockY <= this.world.getMaxHeight() :
                blockY >= 0 && blockY <= PlayerUtils.maxHeight) {
            if (PluginBase.packetsEnabled()) {
                if (PluginBase.isSynchronised()) {
                    return setBlock();
                } else {
                    return setAsyncBlock(protocol);
                }
            } else if (MultiVersion.folia
                    || PluginBase.isSynchronised()) {
                return setBlock();
            } else {
                int x = getChunkX(), z = getChunkZ();

                if (isChunkLoaded(x, z)) {
                    return setBlock();
                } else {
                    return new ServerBlock(null);
                }
            }
        } else {
            return new ServerBlock(emptyMaterial);
        }
    }

    private boolean isChunkLoaded(int x, int z) {
        return ChunksEvent.isLoaded(this.world, x, z);
    }

    public boolean isChunkLoaded() {
        return isChunkLoaded(getChunkX(), getChunkZ());
    }

    public ServerLocation getBlockLocation() {
        ServerLocation location = this.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }

    public double distance(ServerLocation loc) {
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public double distance(Block block) {
        return AlgebraUtils.getDistance(this.x, block.getX(), this.y, block.getY(), this.z, block.getZ());
    }

    public double distanceSquared(ServerLocation loc) {
        return AlgebraUtils.getSquaredDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public double distance(Location loc) {
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public Location bukkit() {
        return new Location(this.world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    // Custom

    // Direction

    public boolean changedDirection(ServerLocation location) {
        return getPitch() != location.getPitch() || getYaw() != location.getYaw();
    }

    public boolean changedBlock(ServerLocation location, boolean elevated) {
        return getBlockX() != location.getBlockX() || getBlockZ() != location.getBlockZ()
                || (elevated ? getBlockY() > location.getBlockY() : getBlockY() != location.getBlockY());
    }

    public BlockFace getDirectionFace() {
        float yaw = getYaw();

        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        yaw %= 360;

        switch ((int) ((yaw + 8) / 22.5)) {
            case 1:
                return BlockFace.WEST_NORTH_WEST;
            case 2:
                return BlockFace.NORTH_WEST;
            case 3:
                return BlockFace.NORTH_NORTH_WEST;
            case 4:
                return BlockFace.NORTH;
            case 5:
                return BlockFace.NORTH_NORTH_EAST;
            case 6:
                return BlockFace.NORTH_EAST;
            case 7:
                return BlockFace.EAST_NORTH_EAST;
            case 8:
                return BlockFace.EAST;
            case 9:
                return BlockFace.EAST_SOUTH_EAST;
            case 10:
                return BlockFace.SOUTH_EAST;
            case 11:
                return BlockFace.SOUTH_SOUTH_EAST;
            case 12:
                return BlockFace.SOUTH;
            case 13:
                return BlockFace.SOUTH_SOUTH_WEST;
            case 14:
                return BlockFace.SOUTH_WEST;
            case 15:
                return BlockFace.WEST_SOUTH_WEST;
            default:
                return BlockFace.WEST;
        }
    }

    // Surrounding

    public Collection<ServerLocation> getSurroundingLocations(double x, double y, double z) {
        if (x >= 1.0 || z >= 1.0) {
            return this.getRawSurroundingLocations(x, y, z).values();
        } else {
            double endX = 1.0 - x, endZ = 1.0 - z;

            if (x != endX && z != endZ) {
                double xBox = getX() - getBlockX(),
                        zBox = getZ() - getBlockZ();

                if (xBox > x && xBox < endX
                        && zBox > z && zBox < endZ) {
                    List<ServerLocation> locations = new ArrayList<>(2);
                    locations.add(this.clone().add(0, y, 0));
                    return locations;
                } else {
                    return this.getRawSurroundingLocations(x, y, z).values();
                }
            } else {
                return this.getRawSurroundingLocations(x, y, z).values();
            }
        }
    }

    private Map<Integer, ServerLocation> getRawSurroundingLocations(double x, double y, double z) {
        Map<Integer, ServerLocation> locations = new LinkedHashMap<>(10, 1.0f);
        ServerLocation yOnly = this.clone().add(0, y, 0);
        locations.put(
                locationIdentifier(yOnly.getBlockX(), yOnly.getBlockY(), yOnly.getBlockZ()),
                yOnly
        );

        for (double[] positions : new double[][]{
                {x, 0.0},
                {-x, 0.0},
                {0.0, z},
                {0.0, -z},
                {x, z},
                {-x, -z},
                {x, -z},
                {-x, z}
        }) {
            ServerLocation location = this.clone().add(positions[0], y, positions[1]);
            locations.putIfAbsent(
                    locationIdentifier(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    location
            );
        }
        return locations;
    }

    @Deprecated
    private ServerLocation[] getSurroundingLocations_DEPRECATED(double x, double y, double z, boolean all) {
        ServerLocation[] array;

        if (x > 0.0 || z > 0.0) {
            double dx = getX() - getBlockX(),
                    dz = getZ() - getBlockZ();

            if (!all) {
                if (dx >= 0.0 && dx < 0.3 && dz >= 0.0 && dz < 0.3) {
                    // 300 300
                    array = new ServerLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, -z);
                    array[2] = this.clone().add(-x, y, 0);
                    array[3] = this.clone().add(-x, y, -z);
                } else if (dx > 0.7 && dx < 1.0 && dz >= 0.0 && dz < 0.3) {
                    // 700 300
                    array = new ServerLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                    array[2] = this.clone().add(0, y, -z);
                    array[3] = this.clone().add(x, y, -z);
                } else if (dx > 0.7 && dx < 1.0 && dz > 0.7 && dz < 1.0) {
                    // 700 700
                    array = new ServerLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                    array[2] = this.clone().add(0, y, z);
                    array[3] = this.clone().add(x, y, z);
                } else if (dx >= 0.0 && dx < 0.3 && dz > 0.7 && dz < 1.0) {
                    // 300 700
                    array = new ServerLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(-x, y, 0);
                    array[2] = this.clone().add(0, y, z);
                    array[3] = this.clone().add(-x, y, z);
                } else if (dx >= 0.0 && dx < 0.3 && dz > 0.3 && dz < 0.7) {
                    // 300 300-700
                    array = new ServerLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                } else if (dx > 0.7 && dx < 1.0 && dz > 0.3 && dz < 0.7) {
                    // 700 300-700
                    array = new ServerLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(-x, y, 0);
                } else if (dx > 0.3 && dx < 0.7 && dz >= 0.0 && dz < 0.3) {
                    // 300-700 300
                    array = new ServerLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, -z);
                } else if (dx > 0.3 && dx < 0.7 && dz > 0.7 && dz < 1.0) {
                    // 300-700 700
                    array = new ServerLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, z);
                } else {
                    array = new ServerLocation[1];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                }
            } else {
                array = new ServerLocation[9];
                array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                array[1] = this.clone().add(x, y, 0);
                array[2] = this.clone().add(-x, y, 0);
                array[3] = this.clone().add(0, y, z);
                array[4] = this.clone().add(0, y, -z);
                array[5] = this.clone().add(x, y, z);
                array[6] = this.clone().add(-x, y, -z);
                array[7] = this.clone().add(x, y, -z);
                array[8] = this.clone().add(-x, y, z);
            }
        } else {
            array = new ServerLocation[1];
            array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
        }
        return array;
    }

}
