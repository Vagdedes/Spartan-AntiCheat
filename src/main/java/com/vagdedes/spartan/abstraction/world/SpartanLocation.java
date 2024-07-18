package com.vagdedes.spartan.abstraction.world;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.chunks.Event_Chunks;
import com.vagdedes.spartan.listeners.bukkit.chunks.Event_Chunks_v1_13;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector3D;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector3F;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.*;

public class SpartanLocation implements Cloneable {

    private static final boolean
            v_1_13 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            v_1_17 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    public static int getChunkPos(int pos) {
        return pos >> 4;
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
        this.time = System.currentTimeMillis();
    }

    public SpartanLocation(Location loc, float yaw, float pitch) { // Used for vehicles
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);
    }

    public SpartanLocation(Location loc) { // Used globally
        this(loc, loc.getYaw(), loc.getPitch());
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

    public SpartanVector3D toVector3D() {
        return new SpartanVector3D(this.x, this.y, this.z);
    }

    public SpartanVector3F toVector3F() {
        return new SpartanVector3F((float) this.x, (float) this.y, (float) this.z);
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
        Block block = this.world.getBlockAt(getBlockX(), getBlockY(), getBlockZ());

        if (block != null) {
            if (SpartanLocation.v_1_13) {
                BlockData blockData = block.getBlockData();
                return new SpartanBlock(
                        this,
                        blockData.getMaterial(),
                        BlockUtils.isLiquid(block),
                        BlockUtils.isWaterLogged(blockData)
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

    private SpartanBlock setAsyncBlock() {
        if (SpartanLocation.v_1_13) {
            BlockData data = Event_Chunks_v1_13.getBlockData(this.world, this.getBlockX(), this.getBlockY(), this.getBlockZ());

            if (data != null) {
                return new SpartanBlock(
                        this,
                        data.getMaterial(),
                        BlockUtils.isLiquid(data.getMaterial()),
                        BlockUtils.isWaterLogged(data)
                );
            } else {
                return new SpartanBlock(this, Material.AIR, false, false);
            }
        } else {
            Material type = Event_Chunks.getBlockType(this.world, this.getBlockX(), this.getBlockY(), this.getBlockZ());

            if (type != null) {
                return new SpartanBlock(
                        this,
                        type,
                        BlockUtils.isLiquid(type),
                        false
                );
            } else {
                return new SpartanBlock(this, Material.AIR, false, false);
            }
        }
    }

    public SpartanBlock getBlock() {
        int blockY = getBlockY();

        if (SpartanLocation.v_1_17 ?
                blockY >= this.world.getMinHeight() && blockY <= this.world.getMaxHeight() :
                blockY >= 0 && blockY <= PlayerUtils.height) {
            if (Event_Chunks.enabled()) {
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
                    SpartanBlock[] block = new SpartanBlock[1];
                    Thread thread = Thread.currentThread();

                    SpartanBukkit.transferTask(this.world, x, z, () -> {
                        block[0] = setBlock();

                        synchronized (thread) {
                            thread.notifyAll();
                        }
                    });
                    synchronized (thread) {
                        if (block[0] == null) {
                            try {
                                thread.wait();
                            } catch (Exception ex) {
                                block[0] = new SpartanBlock(this, Material.AIR, false, false);
                                SpartanBukkit.transferTask(this.world, x, z, () -> block[0] = setBlock());
                            }
                        }
                    }
                    return block[0];
                }
            }
        } else {
            return new SpartanBlock(this, Material.AIR, false, false);
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
