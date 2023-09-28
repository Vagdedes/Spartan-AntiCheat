package me.vagdedes.spartan.gui;

import me.vagdedes.spartan.abstraction.InventoryMenu;
import me.vagdedes.spartan.functionality.moderation.PlayerReports;
import me.vagdedes.spartan.gui.configuration.ManageChecks;
import me.vagdedes.spartan.gui.configuration.ManageConfiguration;
import me.vagdedes.spartan.gui.configuration.ManageOptions;
import me.vagdedes.spartan.gui.info.DebugMenu;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.gui.spartan.MainMenu;
import me.vagdedes.spartan.gui.spartan.SupportIncompatibleItems;

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
