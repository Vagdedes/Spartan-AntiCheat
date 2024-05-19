package com.vagdedes.spartan.listeners.protocol.modules;

public class AbilitiesContainer {
    private boolean sprint;
    private boolean sneak;
    private boolean fly;


    public AbilitiesContainer(boolean sprint, boolean fly, boolean sneak) {
        this.sprint = sprint;
        this.fly = fly;
        this.sneak = sneak;
    }

    public boolean sprint() {
        return this.sprint;
    }

    public boolean fly() {
        return this.fly;
    }

    public boolean sneak() {
        return this.sneak;
    }

    public void setSprint(boolean isSprinting) {
        this.sprint = isSprinting;
    }

    public void setFly(boolean isFlying) {
        this.fly = isFlying;
    }

    public void setSneak(boolean isSneaking) {
        this.sneak = isSneaking;
    }
}
