package com.vagdedes.spartan.abstraction.protocol;

public class RotationData extends SpartanProtocolField {

    private float yaw;
    private float pitch;
    private boolean has;

    RotationData() {
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.has = false;
    }

    @Override
    public boolean hasData() {
        return has;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.has = true;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.has = true;
    }
}
