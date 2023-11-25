package me.vagdedes.spartan.functionality.protections;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Explosion {

    private static final int defaultTicks = 4 * 20;
    private static final long expirationTime = 3 * 50L;
    private static final Map<Location, Long> memory = Cache.store(new LinkedHashMap<>());

    private static void clear() {
        Iterator<Long> iterator = memory.values().iterator();
        long time = System.currentTimeMillis();

        while (iterator.hasNext()) {
            if (time - iterator.next() > expirationTime) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    // Separator

    private static void run(SpartanPlayer p) {
        if (!Config.settings.exists(Settings.explosionOption)) {
            Config.settings.setOption(Settings.explosionOption, Explosion.defaultTicks / 20);
        }
        p.getHandlers().add(
                Handlers.HandlerType.Explosion, getTime()
        );
    }

    public static void runDamage(SpartanPlayer player, Entity damager, EntityDamageEvent.DamageCause dmg) {
        if (dmg == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || dmg == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && !(damager instanceof Firework) && !(damager instanceof Creeper)) {
            run(player);
        }
    }

    // Separator

    public static void runExplosion(Block block) { // We iterate the clearance here because the velocity event won't always be called
        if (memory.size() > 0) {
            clear();
        }
        memory.put(block.getLocation(), System.currentTimeMillis() + expirationTime);
    }

    public static void runVelocity(SpartanPlayer player) {
        if (memory.size() > 0) {
            clear();
            SpartanLocation to = player.getLocation();
            World world = to.getWorld();

            for (Location explosion : memory.keySet()) {
                World explosionWorld = explosion.getWorld();

                if (explosionWorld != null
                        && explosionWorld.equals(world)
                        && to.distance(explosion) <= CombatUtils.maxHitDistance) {
                    run(player);
                    break;
                }
            }
        }
    }

    // Separator

    public static boolean justExploded(SpartanPlayer p) {
        return p.getHandlers().getRemainingTicks(Handlers.HandlerType.Explosion) >= (getTime() - 2);
    }

    private static int getTime() {
        return TestServer.isIdentified() ? defaultTicks :
                (Math.max(Config.settings.getInteger(Settings.explosionOption), defaultTicks) * 20);
    }
}
