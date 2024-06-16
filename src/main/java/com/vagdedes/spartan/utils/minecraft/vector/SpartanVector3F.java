package com.vagdedes.spartan.utils.minecraft.vector;

public class SpartanVector3F {

    public float x, y, z;

    public SpartanVector3F(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SpartanVector3F sub(SpartanVector3F v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }
}
