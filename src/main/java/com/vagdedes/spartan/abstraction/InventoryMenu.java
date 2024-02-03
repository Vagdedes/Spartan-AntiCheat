package com.vagdedes.spartan.abstraction;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class InventoryMenu {

    protected String title;

    protected final Enums.Permission[] permissions;
    protected ItemStack itemStack;
    protected ClickType clickType;
    protected int slot, size;
    protected Inventory inventory;

    public InventoryMenu(String title, int size, Enums.Permission[] permissions) {
        this.title = title;
        this.itemStack = null;
        this.clickType = null;
        this.inventory = null;
        this.slot = 0;
        this.size = size;
        this.permissions = permissions;
    }

    public InventoryMenu(String title, int size, Enums.Permission permissions) {
        this(title, size, new Enums.Permission[]{permissions});
    }

    protected Inventory setInventory(SpartanPlayer player, String title, int size) {
        this.title = title;
        this.size = size;
        return inventory = player.createInventory(size, title);
    }

    protected Inventory setSize(SpartanPlayer player, int size) {
        this.size = size;
        return inventory = player.createInventory(size, title);
    }

    protected Inventory setTitle(SpartanPlayer player, String title) {
        this.title = title;
        return inventory = player.createInventory(size, title);
    }

    protected void add(String name, List<String> lore, ItemStack item, int slot) {
        InventoryUtils.add(inventory, name, lore, item, slot);
    }

    public boolean open(SpartanPlayer player, boolean permissionMessage) {
        return open(player, permissionMessage, null);
    }

    public boolean open(SpartanPlayer player, Object object) {
        return open(player, true, object);
    }

    public boolean open(SpartanPlayer player) {
        return open(player, true, null);
    }

    public boolean open(SpartanPlayer player, boolean permissionMessage, Object object) {
        boolean access;

        if (permissions.length == 0) {
            access = true;
        } else {
            boolean check = false;

            for (Enums.Permission permission : permissions) {
                if (Permissions.has(player, permission)) {
                    check = true;
                    break;
                }
            }
            access = check;
        }

        if (access) {
            inventory = player.createInventory(size, title);
            if (internalOpen(player, permissionMessage, object)) {
                player.openInventory(inventory);
                return true;
            } else {
                return false;
            }
        } else {
            player.sendInventoryCloseMessage(
                    permissionMessage ? Config.messages.getColorfulString("no_permission") : null
            );
            return false;
        }
    }

    protected abstract boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object);

    public boolean handle(SpartanPlayer player, String title, ItemStack itemStack, ClickType clickType, int slot) {
        if (title.equals(this.title)) {
            boolean access;

            if (permissions.length == 0) {
                access = true;
            } else {
                boolean check = false;

                for (Enums.Permission permission : permissions) {
                    if (Permissions.has(player, permission)) {
                        check = true;
                        break;
                    }
                }
                access = check;

                if (!access) {
                    player.sendInventoryCloseMessage(null);
                }
            }

            if (access) {
                this.itemStack = itemStack;
                this.clickType = clickType;
                this.slot = slot;
                return internalHandle(player);
            }
        }
        return false;
    }

    protected abstract boolean internalHandle(SpartanPlayer player);
}
