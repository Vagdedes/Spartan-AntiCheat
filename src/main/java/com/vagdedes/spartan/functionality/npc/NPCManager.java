package com.vagdedes.spartan.functionality.npc;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.World;
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
            if (!list.isEmpty()) {
                List<SpartanProtocol> staff = Permissions.getStaff();

                if (staff.isEmpty()) {
                    clear();
                } else {
                    synchronized (list) {
                        Iterator<SpartanNPC> iterator = list.iterator();

                        while (iterator.hasNext()) {
                            SpartanNPC npc = iterator.next();
                            boolean animate = false;

                            for (SpartanProtocol player : staff) {
                                if (SpartanLocation.distance(npc.location, player.getLocationOrVehicle()) <= PlayerUtils.chunk) {
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

    public static void clear(World world) {
        if (!list.isEmpty()) {
            synchronized (list) {
                for (SpartanNPC npc : list) {
                    if (world.equals(npc.armorStand.getWorld())) {
                        npc.remove();
                    }
                }
                list.clear();
            }
        }
    }

    public static void create(SpartanProtocol protocol) {
        if (supported) {
            Location location = protocol.getLocationOrVehicle();

            if (!list.isEmpty()) {
                synchronized (list) {
                    for (SpartanNPC npc : list) {
                        if (SpartanLocation.distance(npc.location, location) <= PlayerUtils.chunk) {
                            protocol.teleport(npc.location);
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
                            SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());
                            InteractiveInventory.mainMenu.open(protocol, Permissions.has(protocol.bukkit()));
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
                                SpartanProtocol protocol = SpartanBukkit.getProtocol((Player) damager);
                                InteractiveInventory.mainMenu.open(protocol, Permissions.has(protocol.bukkit()));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}
