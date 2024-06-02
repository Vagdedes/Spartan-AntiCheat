package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpartanPotionEffect {

    static Map<PotionEffectType, SpartanPotionEffect> mapFromBukkit(SpartanPlayer player,
                                                                    Collection<PotionEffect> collection) {
        Map<PotionEffectType, SpartanPotionEffect> map = new LinkedHashMap<>(collection.size() + 1, 1.0f);

        for (PotionEffect effect : collection) {
            map.put(effect.getType(), new SpartanPotionEffect(player, effect));
        }
        return map;
    }

    static List<SpartanPotionEffect> listFromBukkit(SpartanPlayer player,
                                                    Collection<PotionEffect> collection) {
        List<SpartanPotionEffect> list = new ArrayList<>(collection.size());

        for (PotionEffect effect : collection) {
            list.add(new SpartanPotionEffect(player, effect));
        }
        return list;
    }

    // Separator

    private final SpartanPlayer parent;
    public final PotionEffect bukkitEffect;
    private final long expiration;

    public SpartanPotionEffect(SpartanPlayer player, PotionEffect effect) {
        this.parent = player;
        this.bukkitEffect = effect;
        this.expiration = TPS.getTick(player) + effect.getDuration();
    }

    public long ticksPassed() {
        long remaining = this.expiration - TPS.getTick(this.parent);
        return remaining >= 0 ? 0 : Math.abs(remaining);
    }

    public boolean isActive() {
        return this.expiration - TPS.getTick(this.parent) >= 0;
    }
}
