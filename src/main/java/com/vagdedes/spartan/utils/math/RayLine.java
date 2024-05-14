package com.vagdedes.spartan.functionality.tracking;

import java.util.Objects;

public class RayLine {

    private final double x;
    private final double z;

    public RayLine(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RayLine rayLine = (RayLine) o;
        return Double.compare(rayLine.x, x) == 0 && Double.compare(rayLine.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "RayLine{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }
}
