package com.vagdedes.spartan.abstraction.world;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_Chunks;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

public class SpartanLocation implements Cloneable {

    public static final Location bukkitDefault = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
    private static final boolean v_1_13 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13);

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
        x = (SpartanBukkit.hashCodeMultiplier * x) + y;
        return (SpartanBukkit.hashCodeMultiplier * x) + z;
    }

    // Object

    private final long time;
    public final World world;
    private double x, y, z;
    private float yaw, pitch;

    // Base

    public SpartanLocation(World world,
                           double x, double y, double z,
                           float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.time = System.currentTimeMillis();
    }

    public SpartanLocation(World world, double x, double y, double z) {
        this(world, x, y, z, 0.0f, 0.0f);
    }

    public SpartanLocation(Location loc) {
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    // Methods

    public SpartanLocation clone() {
        try {
            return (SpartanLocation) super.clone();
        } catch (Exception ex) {
            return null;
        }
    }

    public long timePassed() {
        return System.currentTimeMillis() - this.time;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
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

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
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
        if (Event_Chunks.heightSupport) {
            return Math.max(this.world.getMinHeight(), Math.min(getBlockY(), this.world.getMaxHeight()));
        } else {
            return Math.max(0, Math.min(getBlockY(), PlayerUtils.height));
        }
    }

    public int getLocalZ() {
        return getBlockZ() & 15;
    }

    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }

    public SpartanLocation subtract(double x, double y, double z) {
        return add(-x, -y, -z);
    }

    public SpartanLocation subtract(SpartanLocation loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation subtract(Location loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation subtract(Vector vec) {
        return subtract(vec.getX(), vec.getY(), vec.getZ());
    }

    public SpartanLocation add(Vector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public SpartanLocation add(SpartanLocation loc) {
        return add(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation add(double x, double y, double z) {
        Location bukkit = new Location(this.world, this.x, this.y, this.z).add(x, y, z);
        this.x = bukkit.getX();
        this.y = bukkit.getY();
        this.z = bukkit.getZ();
        return this;
    }

    private SpartanBlock setBlock() {
        return new SpartanBlock(this.world.getBlockAt(getBlockX(), getBlockY(), getBlockZ()));
    }

    private SpartanBlock setAsyncBlock() {
        return new SpartanBlock(
                Event_Chunks.getBlockAsync(this.bukkit())
        );
    }

    public SpartanBlock getBlock() {
        int blockY = getBlockY();

        if (Event_Chunks.heightSupport ?
                blockY >= this.world.getMinHeight() && blockY <= this.world.getMaxHeight() :
                blockY >= 0 && blockY <= PlayerUtils.height) {
            if (SpartanBukkit.packetsEnabled()) {
                if (SpartanBukkit.isSynchronised()) {
                    return setBlock();
                } else {
                    return setAsyncBlock();
                }
            } else if (MultiVersion.folia
                    || SpartanBukkit.isSynchronised()) {
                return setBlock();
            } else {
                int x = getChunkX(), z = getChunkZ();

                if (isChunkLoaded(x, z)) {
                    return setBlock();
                } else {
                    return new SpartanBlock(null);
                }
            }
        } else {
            return new SpartanBlock(Material.AIR);
        }
    }

    private boolean isChunkLoaded(int x, int z) {
        return Event_Chunks.isLoaded(this.world, x, z);
    }

    public boolean isChunkLoaded() {
        return isChunkLoaded(getChunkX(), getChunkZ());
    }

    public SpartanLocation getBlockLocation() {
        SpartanLocation location = this.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }

    public double distance(SpartanLocation loc) {
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public double distance(Block block) {
        return AlgebraUtils.getDistance(this.x, block.getX(), this.y, block.getY(), this.z, block.getZ());
    }

    public double distanceSquared(SpartanLocation loc) {
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

    public boolean changedDirection(SpartanLocation location) {
        return getPitch() != location.getPitch() || getYaw() != location.getYaw();
    }

    public boolean changedBlock(SpartanLocation location, boolean elevated) {
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

    public Collection<SpartanLocation> getSurroundingLocations(double x, double y, double z) {
        if (x >= 1.0 || z >= 1.0) {
            return this.getRawSurroundingLocations(x, y, z).values();
        } else {
            double endX = 1.0 - x, endZ = 1.0 - z;

            if (x != endX && z != endZ) {
                double xBox = getX() - getBlockX(),
                        zBox = getZ() - getBlockZ();

                if (xBox > x && xBox < endX
                        && zBox > z && zBox < endZ) {
                    List<SpartanLocation> locations = new ArrayList<>(2);
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

    private Map<Integer, SpartanLocation> getRawSurroundingLocations(double x, double y, double z) {
        Map<Integer, SpartanLocation> locations = new LinkedHashMap<>(10, 1.0f);
        SpartanLocation yOnly = this.clone().add(0, y, 0);
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
            SpartanLocation location = this.clone().add(positions[0], y, positions[1]);
            locations.putIfAbsent(
                    locationIdentifier(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    location
            );
        }
        return locations;
    }

    @Deprecated
    private SpartanLocation[] getSurroundingLocations_DEPRECATED(double x, double y, double z, boolean all) {
        SpartanLocation[] array;

        if (x > 0.0 || z > 0.0) {
            double dx = getX() - getBlockX(),
                    dz = getZ() - getBlockZ();

            if (!all) {
                if (dx >= 0.0 && dx < 0.3 && dz >= 0.0 && dz < 0.3) {
                    // 300 300
                    array = new SpartanLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, -z);
                    array[2] = this.clone().add(-x, y, 0);
                    array[3] = this.clone().add(-x, y, -z);
                } else if (dx > 0.7 && dx < 1.0 && dz >= 0.0 && dz < 0.3) {
                    // 700 300
                    array = new SpartanLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                    array[2] = this.clone().add(0, y, -z);
                    array[3] = this.clone().add(x, y, -z);
                } else if (dx > 0.7 && dx < 1.0 && dz > 0.7 && dz < 1.0) {
                    // 700 700
                    array = new SpartanLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                    array[2] = this.clone().add(0, y, z);
                    array[3] = this.clone().add(x, y, z);
                } else if (dx >= 0.0 && dx < 0.3 && dz > 0.7 && dz < 1.0) {
                    // 300 700
                    array = new SpartanLocation[4];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(-x, y, 0);
                    array[2] = this.clone().add(0, y, z);
                    array[3] = this.clone().add(-x, y, z);
                } else if (dx >= 0.0 && dx < 0.3 && dz > 0.3 && dz < 0.7) {
                    // 300 300-700
                    array = new SpartanLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(x, y, 0);
                } else if (dx > 0.7 && dx < 1.0 && dz > 0.3 && dz < 0.7) {
                    // 700 300-700
                    array = new SpartanLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(-x, y, 0);
                } else if (dx > 0.3 && dx < 0.7 && dz >= 0.0 && dz < 0.3) {
                    // 300-700 300
                    array = new SpartanLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, -z);
                } else if (dx > 0.3 && dx < 0.7 && dz > 0.7 && dz < 1.0) {
                    // 300-700 700
                    array = new SpartanLocation[2];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                    array[1] = this.clone().add(0, y, z);
                } else {
                    array = new SpartanLocation[1];
                    array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
                }
            } else {
                array = new SpartanLocation[9];
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
            array = new SpartanLocation[1];
            array[0] = y == 0.0 ? this : this.clone().add(0, y, 0);
        }
        return array;
    }

}
