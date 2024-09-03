package com.vagdedes.spartan.functionality.npc;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.util.*;

public class NPCManager implements Listener {

    public static final boolean supported = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);
    private static final List<SpartanNPC> list = Collections.synchronizedList(new ArrayList<>());

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            List<SpartanPlayer> staff = Permissions.getStaff();

            if (staff.isEmpty()) {
                clear();
            } else {
                synchronized (list) {
                    Iterator<SpartanNPC> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        SpartanNPC npc = iterator.next();
                        boolean animate = false;

                        for (SpartanPlayer player : staff) {
                            if (npc.location.distance(player.movement.getLocation()) <= PlayerUtils.chunk) {
                                animate = true;
                                break;
                            }
                        }

                        if (animate) {
                            if (!npc.animate(staff)) {
                                iterator.remove();
                            }
                        } else {
                            npc.remove();
                            iterator.remove();
                        }
                    }
                }
            }
        }, 1L, 1L);
    }

    public static void clear() {
        if (!list.isEmpty()) {
            synchronized (list) {
                for (SpartanNPC npc : list) {
                    npc.remove();
                }
                list.clear();
            }
        }
    }

    public static void create(SpartanPlayer player) {
        if (supported) {
            SpartanLocation location = player.movement.getLocation();

            if (!list.isEmpty()) {
                synchronized (list) {
                    for (SpartanNPC npc : list) {
                        if (npc.location.distance(location) <= PlayerUtils.chunk) {
                            player.teleport(npc.location);
                            return;
                        }
                    }
                    list.add(new SpartanNPC(location));
                }
            } else {
                synchronized (list) {
                    list.add(new SpartanNPC(location));
                }
            }
        }
    }

    @EventHandler
    public void Interact(PlayerInteractAtEntityEvent e) {
        Entity entity = e.getRightClicked();

        if (entity instanceof ArmorStand) {
            UUID uuid = entity.getUniqueId();

            if (!list.isEmpty()) {
                synchronized (list) {
                    for (SpartanNPC npc : list) {
                        if (npc.getUniqueId().equals(uuid)) {
                            e.setCancelled(true);
                            npc.updateHead();
                            SpartanPlayer player = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

                            if (player != null) {
                                InteractiveInventory.mainMenu.open(player, Permissions.has(player.getInstance()));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void Damage(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof ArmorStand) {
            UUID uuid = entity.getUniqueId();

            if (!list.isEmpty()) {
                synchronized (list) {
                    for (SpartanNPC npc : list) {
                        if (npc.getUniqueId().equals(uuid)) {
                            e.setCancelled(true);
                            npc.updateHead();
                            Entity damager = e.getDamager();

                            if (damager instanceof Player) {
                                SpartanPlayer player = SpartanBukkit.getProtocol((Player) damager).spartanPlayer;

                                if (player != null) {
                                    InteractiveInventory.mainMenu.open(player, Permissions.has(player.getInstance()));
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}
