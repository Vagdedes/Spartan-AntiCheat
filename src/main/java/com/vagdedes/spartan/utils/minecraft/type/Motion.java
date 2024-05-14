package com.vagdedes.spartan.utils.minecraft.type;

import com.vagdedes.spartan.utils.minecraft.mcp.MathHelper;
import org.bukkit.util.Vector;

public final class Motion {

    private final AdvancedDouble x, y, z;

    /**
     * Create an empty constructor if we do not want initial values for our motion.
     */
    public Motion() {
        this.x = new AdvancedDouble(0.0D);
        this.y = new AdvancedDouble(0.0D);
        this.z = new AdvancedDouble(0.0D);
    }

    /**
     * Set an initial value for our base motion.
     */
    public Motion(final double x, final double y, final double z) {
        this.x = new AdvancedDouble(x);
        this.y = new AdvancedDouble(y);
        this.z = new AdvancedDouble(z);
    }

    /**
     * Set an initial value for our base motion.
     */
    public Motion(final AdvancedDouble x, final AdvancedDouble y, final AdvancedDouble z) {
        this.x = new AdvancedDouble(x.get());
        this.y = new AdvancedDouble(y.get());
        this.z = new AdvancedDouble(z.get());
    }

    public void set(final Vector vector) {
        this.x.set(vector.getX());
        this.y.set(vector.getY());
        this.z.set(vector.getZ());
    }

    public void add(final Vector vector) {
        this.x.add(vector.getX());
        this.y.add(vector.getY());
        this.z.add(vector.getZ());
    }

    public double distanceSquared(final Motion other) {
        return Math.pow(this.x.get() - other.getX().get(), 2) +
                        Math.pow(this.y.get() - other.getY().get(), 2) +
                        Math.pow(this.z.get() - other.getZ().get(), 2);
    }

    public double length() {
        return MathHelper.sqrt_double(this.x.get() * this.x.get()
                        + this.y.get() * this.y.get() + this.z.get() * this.z.get());
    }

    public Motion normalize() {
        final double d0 = this.length();
        return d0 < 1.0E-4D ? new Motion(0.0D, 0.0D, 0.0D) : new Motion(this.x.get() / d0, this.y.get() / d0, this.z.get() / d0);
    }

    public Motion clone() {
        return new Motion(x.get(), y.get(), z.get());
    }

    public AdvancedDouble getX() {
        return this.x;
    }

    public AdvancedDouble getY() {
        return this.y;
    }

    public AdvancedDouble getZ() {
        return this.z;
    }
}
