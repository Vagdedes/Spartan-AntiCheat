package com.vagdedes.spartan.gui;

import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.functionality.moderation.PlayerReports;
import com.vagdedes.spartan.gui.configuration.ManageChecks;
import com.vagdedes.spartan.gui.configuration.ManageConfiguration;
import com.vagdedes.spartan.gui.configuration.ManageOptions;
import com.vagdedes.spartan.gui.info.DebugMenu;
import com.vagdedes.spartan.gui.info.PlayerInfo;
import com.vagdedes.spartan.gui.spartan.MainMenu;
import com.vagdedes.spartan.gui.spartan.SupportIncompatibleItems;

public class SpartanMenu {

    public static final ManageChecks manageChecks = new ManageChecks();
    public static final MainMenu mainMenu = new MainMenu();
    public static final SupportIncompatibleItems supportIncompatibleItems = new SupportIncompatibleItems();
    public static final DebugMenu debugMenu = new DebugMenu();
    public static final PlayerInfo playerInfo = new PlayerInfo();
    public static final ManageConfiguration manageConfiguration = new ManageConfiguration();
    public static final ManageOptions manageOptions = new ManageOptions();
    public static final PlayerReports playerReports = new PlayerReports();

    public static final InventoryMenu[] menus = new InventoryMenu[]{
            manageChecks,
            mainMenu,
            supportIncompatibleItems,
            debugMenu,
            playerInfo,
            manageConfiguration,
            manageOptions,
            playerReports
    };
}
