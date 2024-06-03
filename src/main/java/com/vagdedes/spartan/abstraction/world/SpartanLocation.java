package com.vagdedes.spartan.abstraction.world;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.util.Vector;

import java.util.*;

public class SpartanLocation {

    static final Map<Integer, SpartanBlock> memory = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final boolean
            v_1_13 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            v_1_17 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    public static int getChunkPos(int pos) {
        return pos >> 4;
    }

    private static int locationIdentifier(int world, int x, int y, int z) {
        world = (SpartanBukkit.hashCodeMultiplier * world) + x;
        world = (SpartanBukkit.hashCodeMultiplier * world) + y;
        return (SpartanBukkit.hashCodeMultiplier * world) + z;
    }

    // Object

    int identifier;
    private final long tick;
    public final World world;
    private double x, y, z;
    private float yaw, pitch;
    private Vector vector;

    // Base

    public SpartanLocation(World world,
                           double x, double y, double z,
                           float yaw, float pitch) { // Used for initiation
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.identifier = locationIdentifier(this.world.hashCode(), getBlockX(), getBlockY(), getBlockZ());
        this.tick = TPS.getTick(this);
    }

    public SpartanLocation(Location loc, float yaw, float pitch) { // Used for vehicles
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);
    }

    public SpartanLocation(Location loc) { // Used globally
        this(loc, loc.getYaw(), loc.getPitch());
    }

    private SpartanLocation(SpartanLocation loc) { // Used for copying
        this(loc.world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
        this.vector = loc.vector;
    }

    // Methods

    public SpartanLocation clone() {
        return new SpartanLocation(this);
    }

    public long ticksPassed() {
        return TPS.getTick(this) - this.tick;
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
        this.vector = null;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.vector = null;
    }

    public void setDirection(Vector vector) {
        this.vector = vector;
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
        if (SpartanLocation.v_1_17) {
            return Math.max(this.world.getMinHeight(), Math.min(getBlockY(), this.world.getMaxHeight()));
        } else {
            return Math.max(0, Math.min(getBlockY(), PlayerUtils.height));
        }
    }

    public int getLocalZ() {
        return getBlockZ() & 15;
    }

    public Vector getDirection() {
        return this.vector == null
                ? this.vector = CombatUtils.getDirection(this.yaw, this.pitch)
                : this.vector;
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

        if (Math.abs(x) >= 1.0
                || Math.abs(y) >= 1.0
                || Math.abs(z) >= 1.0) {
            this.x = bukkit.getX();
            this.y = bukkit.getY();
            this.z = bukkit.getZ();
            this.identifier = locationIdentifier(this.world.hashCode(), getBlockX(), getBlockY(), getBlockZ());
        } else {
            int startX = getBlockX(),
                    startY = getBlockY(),
                    startZ = getBlockZ();

            this.x = bukkit.getX();
            this.y = bukkit.getY();
            this.z = bukkit.getZ();
            int blockX = getBlockX(),
                    blockY = getBlockY(),
                    blockZ = getBlockZ();

            if (blockX != startX || blockY != startY || blockZ != startZ) {
                this.identifier = locationIdentifier(this.world.hashCode(), blockX, blockY, blockZ);
            }
        }
        return this;
    }

    private SpartanBlock setBlock() {
        Block block = this.world.getBlockAt(getBlockX(), getBlockY(), getBlockZ());

        if (block != null) {
            if (SpartanLocation.v_1_13) {
                BlockData blockData = block.getBlockData();
                return new SpartanBlock(
                        this,
                        blockData.getMaterial(),
                        BlockUtils.isLiquid(block),
                        blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged()
                );
            } else {
                return new SpartanBlock(
                        this,
                        block.getType(),
                        BlockUtils.isLiquid(block),
                        false
                );
            }
        } else {
            return new SpartanBlock(this, Material.AIR, false, false);
        }
    }

    public SpartanBlock getBlock() {
        int blockY = getBlockY();

        if (SpartanLocation.v_1_17 ?
                blockY >= this.world.getMinHeight() && blockY <= this.world.getMaxHeight() :
                blockY >= 0 && blockY <= PlayerUtils.height) {
            SpartanBlock cache;

            synchronized (SpartanLocation.memory) {
                cache = SpartanLocation.memory.get(this.identifier);
            }

            if (cache != null && cache.ticksPassed() == 0L) {
                return cache;
            } else {
                if (MultiVersion.folia) {
                    cache = setBlock();

                    synchronized (SpartanLocation.memory) {
                        SpartanLocation.memory.put(this.identifier, cache);
                    }
                    return cache;
                } else {
                    SpartanBlock[] block = new SpartanBlock[1];
                    Runnable runnable = () -> block[0] = setBlock();

                    if (SpartanBukkit.isSynchronised()
                            || isChunkLoaded()) {
                        runnable.run();
                    } else {
                        SpartanBukkit.transferTask(this.world, getChunkX(), getChunkZ(), runnable);
                    }
                    synchronized (SpartanLocation.memory) {
                        if (SpartanLocation.memory.put(this.identifier, block[0]) == null
                                && SpartanLocation.memory.size() > 1_000) {
                            Iterator<Integer> iterator = memory.keySet().iterator();
                            iterator.next();
                            iterator.remove();
                        }
                    }
                    return block[0];
                }
            }
        } else {
            return new SpartanBlock(this, Material.AIR, false, false);
        }
    }

    private boolean isChunkLoaded() {
        int x = getChunkX(),
                z = getChunkZ();

        for (Chunk chunk : this.world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                return true;
            }
        }
        return false;
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

    public double distance(SpartanBlock block) {
        return AlgebraUtils.getDistance(this.x, block.getX(), this.y, block.getY(), this.z, block.getZ());
    }

    public double distanceSquared(SpartanLocation loc) {
        return AlgebraUtils.getSquaredDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public double distance(Location loc) {
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public Location getBukkitLocation() {
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

    // todo block idea where you check x and z and if within 301 and 699 then you don't check surroundings
    public Collection<SpartanLocation> getSurroundingLocations(double x, double y, double z) {
        Map<Integer, SpartanLocation> locations = new LinkedHashMap<>(10, 1.0f);
        SpartanLocation yOnly = this.clone().add(0, y, 0);
        locations.put(yOnly.identifier, yOnly);

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
            locations.putIfAbsent(location.identifier, location);
        }
        return locations.values();
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
