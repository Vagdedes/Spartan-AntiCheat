package com.vagdedes.spartan.abstraction.protocol;

public class AbilitiesContainer {

    private boolean sprint, sneak, fly;

    AbilitiesContainer(boolean sprint, boolean fly, boolean sneak) {
        this.sprint = sprint;
        this.fly = fly;
        this.sneak = sneak;
    }

    public boolean isSprinting() {
        return this.sprint;
    }

    public boolean isFlying() {
        return this.fly;
    }

    public boolean isSneaking() {
        return this.sneak;
    }

    public void setSprinting(boolean isSprinting) {
        this.sprint = isSprinting;
    }

    public void setFlying(boolean isFlying) {
        this.fly = isFlying;
    }

    public void setSneaking(boolean isSneaking) {
        this.sneak = isSneaking;
    }

}
