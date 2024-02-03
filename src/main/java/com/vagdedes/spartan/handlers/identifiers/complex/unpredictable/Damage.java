package com.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class Damage {

    public static final String
            selfHitKey = "self-hit",
            knockbackKey = "knockback",
            fishingHookKey = "fishing-hook";
    private static final int
            blocksOffGround = 4,
            selfHitTicks = 25;
    private static final String
            lastReceivedKey = "last-received",
            lastDealtKey = "=last-dealt",
            projectileSpamKey = "projectile-spam";

    // Handlers

    public static void extremeDamageHandling(SpartanPlayer p) {
        int time = !p.isOnGround() || !p.isOnGroundCustom() ? 120 : 60;
        Velocity.addCooldown(p, -time);
        addCooldown(p, time);
    }

    private static boolean selfHitHandling(SpartanPlayer p, Entity e) {
        boolean result;

        if (e == null) {
            result = true;
        } else if (p.getVehicle() == null && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)) {
            if (Damage.hasCooldown(p)
                    || Math.max(p.getCustomDistance(), p.getValueOrDefault(p.getNmsDistance(), 0.0)) >= 0.18
                    || !p.getHandlers().has(Handlers.HandlerType.Damage, selfHitKey + "=" + e.getUniqueId())
                    || p.getItemInHand().containsEnchantment(Enchantment.ARROW_KNOCKBACK)) {
                result = true;
            } else {
                SpartanLocation to = p.getLocation();

                if (p.wasInLiquids()
                        || !p.isOnGroundCustom() && !PlayerData.isOnGround(p, to, 0, -1, 0)) {
                    result = true;
                } else {
                    result = false;
                }
            }
        } else {
            result = false;
        }

        if (result) {
            addCooldown(p, selfHitKey, selfHitTicks);
        }
        return result;
    }

    // Runnables

    public static void runReceivedDamage(SpartanPlayer p, DamageCause dmg) {
        if (dmg == DamageCause.ENTITY_ATTACK || dmg == DamageCause.PROJECTILE) {
            p.getTimer().set(lastReceivedKey);
        }
    }

    public static void runDealtDamage(SpartanPlayer p) {
        p.getTimer().set(lastDealtKey);
    }

    public static void runReceiveDamage(DamageCause cause, SpartanPlayer p, Entity damager) {
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            if (damager instanceof EnderDragon
                    || damager instanceof Fireball
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && damager instanceof Warden) {
                extremeDamageHandling(p);
            } else if (damager instanceof Golem) {
                addCooldown(p, 80);
            } else if (damager instanceof Player) {
                SpartanPlayer t = SpartanBukkit.getPlayer((Player) damager);

                if (t == null) {
                    addCooldown(p, 40);
                } else {
                    int level = t.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

                    if (level > 2) {
                        int ticks = Math.min(level * 20, 100);
                        Velocity.addCooldown(p, -ticks);
                        addCooldown(p, knockbackKey, ticks);
                    } else if (level > 0) {
                        addCooldown(p, knockbackKey, 60);
                    } else {
                        addCooldown(p, 40);
                    }
                }
            } else {
                addCooldown(p, 30);
            }
        }
    }

    public static boolean runDealAndReceiveDamage(Entity damager, Entity entity, DamageCause dmg, boolean cancelled) {
        if (!cancelled && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) && damager instanceof Player
                && dmg == DamageCause.ENTITY_ATTACK && entity instanceof Guardian) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) damager);

            if (p != null) {
                addCooldown(p, 40);
            }
        } else if (damager instanceof Projectile && entity instanceof Player && dmg == DamageCause.PROJECTILE) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return false;
            }
            Projectile projectile = (Projectile) damager;

            if (projectile.getShooter() instanceof Player) {
                Player nt = ((Player) projectile.getShooter());
                SpartanPlayer t = SpartanBukkit.getPlayer(nt);

                if (t == null) {
                    return false;
                }
                ItemStack item = t.getItemInHand();
                int enchantment = Math.max(item.getEnchantmentLevel(Enchantment.KNOCKBACK), item.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK));

                if (enchantment > 0) {
                    boolean can;

                    if (t.equals(p)) {
                        Buffer buffer = p.getBuffer();
                        Handlers handlers = p.getHandlers();
                        SpartanLocation to = p.getLocation();

                        if (p.isOnGroundCustom() || PlayerData.isOnGround(p, to, 0, -1, 0) || PlayerData.isOnGround(p, to, 0, -2, 0)) {
                            buffer.remove(projectileSpamKey);
                            can = true;
                        } else {
                            can = buffer.increase(projectileSpamKey, 1) < 5 // threshold hasn't been met
                                    || !handlers.has(Handlers.HandlerType.Damage, projectileSpamKey); // time has passed
                        }
                        handlers.add(Handlers.HandlerType.Damage, projectileSpamKey, 40);
                    } else {
                        can = true;
                    }

                    if (can) {
                        int ticks = 80;
                        addCooldown(p, knockbackKey, ticks);

                        if (enchantment > 2) {
                            Velocity.addCooldown(p, -ticks);
                        }
                    }
                } else if (!t.equals(p)) {
                    addCooldown(p, 40);
                }

                // Do not connect with else statement
                if (!selfHitHandling(p, damager)) {
                    return Config.settings.getBoolean("Protections.avoid_self_bow_damage") && !Permissions.isBypassing(p, null);
                }
            } else if (projectile.getShooter() != null) {
                if (projectile.getShooter() instanceof EnderDragon) {
                    int ticks = 60;
                    Velocity.addCooldown(p, -ticks);
                    addCooldown(p, ticks);
                } else {
                    addCooldown(p, 30);
                }
            } else {
                p.setAirTicks(0);
                selfHitHandling(p, null);
            }
        }
        return false;
    }

    public static void runMove(SpartanPlayer p) {
        if (p.getHandlers().has(Handlers.HandlerType.Damage, projectileSpamKey)
                && p.getBlocksOffGround(3, true, true) <= 2) {
            p.getBuffer().remove(projectileSpamKey);
        }
    }

    public static void runBow(Entity entity, Entity projectile) {
        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p != null && p.getLocation().getPitch() <= -60.0f) {
                p.getHandlers().add(Handlers.HandlerType.Damage, selfHitKey + "=" + projectile.getUniqueId(), 200);
            }
        }
    }

    // Return Methods

    public static boolean hasCooldown(SpartanPlayer p, String key) {
        boolean notNull = key != null;

        if (notNull ? p.getHandlers().has(Handlers.HandlerType.Damage, key) : p.getHandlers().has(Handlers.HandlerType.Damage)) {
            if (notNull && key.equals(selfHitKey)
                    || PlayerData.hasLevitationEffect(p)) {
                return true;
            }
            Double vertical = p.getNmsVerticalDistance();

            if (vertical != null && (p.isFalling(vertical) || p.isJumping(vertical))) {
                return true;
            }
            int limit = NoHitDelay.hasCooldown(p) ? (blocksOffGround * 2) : blocksOffGround;
            return p.getBlocksOffGround(limit + 1, true, true) <= limit;
        }
        return false;
    }

    public static boolean hasCooldown(SpartanPlayer p) {
        return hasCooldown(p, null);
    }

    // Modification Methods

    public static void addCooldown(SpartanPlayer p, String reason, int time) {
        Handlers handlers = p.getHandlers();
        handlers.add(Handlers.HandlerType.Damage, time);

        if (reason != null) {
            handlers.add(Handlers.HandlerType.Damage, reason, time);
        }
    }

    public static void addCooldown(SpartanPlayer p, int time) {
        addCooldown(p, null, time);
    }

    // Custom Return Methods

    public static long getLastReceived(SpartanPlayer p) {
        return p.getTimer().get(lastReceivedKey);
    }

    public static long getLastDealt(SpartanPlayer p) {
        return p.getTimer().get(lastDealtKey);
    }
}
