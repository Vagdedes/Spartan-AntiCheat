package com.vagdedes.spartan.compatibility.manual.abilities.crackshot;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponPreShootEvent;
import com.shampaggon.crackshot.events.WeaponScopeEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.LinkedHashMap;

public class CrackShot implements Listener {

    private static final Buffer buffers = new Buffer(
            new OverflowMap<>(new LinkedHashMap<>(), 512)
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WeaponScope(WeaponScopeEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

            if (!e.isCancelled()) {
                Config.compatibility.evadeFalsePositives(
                        protocol.spartan,
                        Compatibility.CompatibilityType.CRACK_SHOT,
                        new Enums.HackCategoryType[]{
                                Enums.HackCategoryType.MOVEMENT,
                                Enums.HackCategoryType.COMBAT
                        },
                        20
                );

                if (e.isZoomIn()) {
                    buffers.set(protocol.getUUID() + "=crackshot=compatibility=scope", 1);
                } else {
                    buffers.remove(protocol.getUUID() + "=crackshot=compatibility=scope");
                }
            } else {
                buffers.remove(protocol.getUUID() + "=crackshot=compatibility=scope");
            }
        }
    }

    @EventHandler
    private void WeaponPreShoot(WeaponPreShootEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;
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
            SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;

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
                SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartan;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityDamage(EntityDamageEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()) {
            Entity entity = e.getEntity();

            if (entity instanceof Player) {
                SpartanProtocol p = SpartanBukkit.getProtocol((Player) entity);

                if (isUsingScope(p)) {
                    Config.compatibility.evadeFalsePositives(
                            p.spartan,
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
                SpartanProtocol p = SpartanBukkit.getProtocol((Player) entity);

                if (isUsingScope(p)) {
                    Config.compatibility.evadeFalsePositives(
                            p.spartan,
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

    public static boolean isUsingScope(SpartanProtocol p) {
        return Compatibility.CompatibilityType.CRACK_SHOT.isFunctional()
                && buffers.get(p.getUUID() + "=crackshot=compatibility=scope") != 0
                || CrackShotPlus.isUsingScope(p);
    }
}
