package com.vagdedes.spartan.abstraction.inventory;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
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

    protected Inventory setInventory(SpartanProtocol protocol, String title, int size) {
        this.title = title;
        this.size = size;
        return inventory = protocol.spartan.createInventory(size, title);
    }

    protected Inventory setSize(SpartanProtocol protocol, int size) {
        this.size = size;
        return inventory = protocol.spartan.createInventory(size, title);
    }

    protected Inventory setTitle(SpartanProtocol protocol, String title) {
        this.title = title;
        return inventory = protocol.spartan.createInventory(size, title);
    }

    protected void add(String name, List<String> lore, ItemStack item, int slot) {
        InventoryUtils.add(inventory, name, lore, item, slot);
    }

    public boolean open(SpartanProtocol protocol, boolean permissionMessage) {
        return open(protocol, permissionMessage, null);
    }

    public boolean open(SpartanProtocol protocol, Object object) {
        return open(protocol, true, object);
    }

    public boolean open(SpartanProtocol protocol) {
        return open(protocol, true, null);
    }

    public boolean open(SpartanProtocol protocol, boolean permissionMessage, Object object) {
        boolean access;

        if (permissions.length == 0) {
            access = true;
        } else {
            boolean check = false;

            for (Enums.Permission permission : permissions) {
                if (Permissions.has(protocol.bukkit(), permission)) {
                    check = true;
                    break;
                }
            }
            access = check;
        }

        if (access) {
            inventory = protocol.spartan.createInventory(size, title);
            if (internalOpen(protocol, permissionMessage, object)) {
                SpartanBukkit.transferTask(
                        protocol,
                        () -> protocol.bukkit().openInventory(inventory)
                );
                return true;
            } else {
                return false;
            }
        } else {
            if (permissionMessage) {
                protocol.bukkit().sendMessage(Config.messages.getColorfulString("no_permission"));
            }
            SpartanBukkit.transferTask(protocol, protocol.bukkit()::closeInventory);
            return false;
        }
    }

    protected abstract boolean internalOpen(SpartanProtocol protocol, boolean permissionMessage, Object object);

    public boolean handle(SpartanProtocol protocol, String title, ItemStack itemStack, ClickType clickType, int slot) {
        if (title.equals(this.title)) {
            boolean access;

            if (permissions.length == 0) {
                access = true;
            } else {
                boolean check = false;

                for (Enums.Permission permission : permissions) {
                    if (Permissions.has(protocol.bukkit(), permission)) {
                        check = true;
                        break;
                    }
                }
                access = check;

                if (!access) {
                    protocol.bukkit().closeInventory();
                }
            }

            if (access) {
                this.itemStack = itemStack;
                this.clickType = clickType;
                this.slot = slot;
                return internalHandle(protocol);
            }
        }
        return false;
    }

    protected abstract boolean internalHandle(SpartanProtocol protocol);
}
