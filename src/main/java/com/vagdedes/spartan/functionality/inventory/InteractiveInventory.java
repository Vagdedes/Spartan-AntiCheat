package com.vagdedes.spartan.functionality.inventory;

import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.inventory.implementation.ManageChecks;
import com.vagdedes.spartan.abstraction.inventory.implementation.PlayerInfo;

public class InteractiveInventory {

    public static final ManageChecks manageChecks = new ManageChecks();
    public static final MainMenu mainMenu = new MainMenu();
    public static final PlayerInfo playerInfo = new PlayerInfo();
    public static final InventoryMenu[] menus = new InventoryMenu[]{
            manageChecks,
            mainMenu,
            playerInfo
    };
}
