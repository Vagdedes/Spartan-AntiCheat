package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.potion.PotionEffect;

public class ExtendedPotionEffect {

    public final PotionEffect bukkitEffect;
    private final long expiration;

    public ExtendedPotionEffect(PotionEffect effect) {
        this.bukkitEffect = effect;
        this.expiration = System.currentTimeMillis() + (effect.getDuration() * TPS.tickTime);
    }

    public long timePassed() {
        long remaining = this.expiration - System.currentTimeMillis();
        return remaining >= 0 ? 0 : Math.abs(remaining);
    }

    public boolean isActive() {
        return this.expiration - System.currentTimeMillis() >= 0;
    }
}
