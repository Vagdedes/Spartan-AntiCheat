package com.vagdedes.spartan.utils.minecraft.vector;

public class SpartanVector3D {

    public double x, y, z;

    public SpartanVector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SpartanVector3D sub(SpartanVector3D v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }
}
