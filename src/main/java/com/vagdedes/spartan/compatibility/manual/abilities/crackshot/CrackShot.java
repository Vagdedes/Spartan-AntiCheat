package com.vagdedes.spartan.compatibility.manual.abilities.crackshot;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponPreShootEvent;
import com.shampaggon.crackshot.events.WeaponScopeEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CrackShot implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WeaponScope(WeaponScopeEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            if (!e.isCancelled()) {
                Config.compatibility.evadeFalsePositives(
                        p,
                        Compatibility.CompatibilityType.CRACK_SHOT,
                        new Enums.HackCategoryType[]{
                                Enums.HackCategoryType.MOVEMENT,
                                Enums.HackCategoryType.COMBAT
                        },
                        20
                );

                if (e.isZoomIn()) {
                    p.buffer.set("crackshot=compatibility=scope", 1);
                } else {
                    p.buffer.remove("crackshot=compatibility=scope");
                }
            } else {
                p.buffer.remove("crackshot=compatibility=scope");
            }
        }
    }

    @EventHandler
    private void WeaponPreShoot(WeaponPreShootEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            Config.compatibility.evadeFalsePositives(
                    p,
                    Compatibility.CompatibilityType.CRACK_SHOT,
                    new Enums.HackCategoryType[]{
                            Enums.HackCategoryType.MOVEMENT,
                            Enums.HackCategoryType.COMBAT
                    },
                    40
            );
        }
    }

    @EventHandler
    private void WeaponShoot(WeaponShootEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            Config.compatibility.evadeFalsePositives(
                    p,
                    Compatibility.CompatibilityType.CRACK_SHOT,
                    new Enums.HackCategoryType[]{
                            Enums.HackCategoryType.MOVEMENT,
                            Enums.HackCategoryType.COMBAT
                    },
                    40
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WeaponDamageEntity(WeaponDamageEntityEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            Entity entity = e.getVictim();

            if (entity instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                if (p != null) {
                    Config.compatibility.evadeFalsePositives(
                            p,
                            Compatibility.CompatibilityType.CRACK_SHOT,
                            new Enums.HackCategoryType[]{
                                    Enums.HackCategoryType.MOVEMENT,
                                    Enums.HackCategoryType.COMBAT
                            },
                            60
                    );
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityDamage(EntityDamageEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            Entity entity = e.getEntity();

            if (entity instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                if (p != null && isUsingScope(p)) {
                    Config.compatibility.evadeFalsePositives(
                            p,
                            Compatibility.CompatibilityType.CRACK_SHOT,
                            new Enums.HackCategoryType[]{
                                    Enums.HackCategoryType.MOVEMENT,
                                    Enums.HackCategoryType.COMBAT
                            },
                            60
                    );
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityByEntityDamage(EntityDamageByEntityEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            Entity entity = e.getDamager();

            if (entity instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                if (p != null && isUsingScope(p)) {
                    Config.compatibility.evadeFalsePositives(
                            p,
                            Compatibility.CompatibilityType.CRACK_SHOT,
                            new Enums.HackCategoryType[]{
                                    Enums.HackCategoryType.MOVEMENT,
                                    Enums.HackCategoryType.COMBAT
                            },
                            60
                    );
                }
            }
        }
    }

    public static boolean isUsingScope(SpartanPlayer p) {
        return Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()
                && p.buffer.get("crackshot=compatibility=scope") != 0
                || CrackShotPlus.isUsingScope(p);
    }
}
