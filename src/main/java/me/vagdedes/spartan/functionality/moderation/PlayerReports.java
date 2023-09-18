package me.vagdedes.spartan.functionality.moderation;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerReport;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerReports {

    private static final String menu = "Report: ";
    public static final String[] reasons = new String[]{
            "Hacking", "Offensive Comments", "Bad Sportsmanship", "Mechanism Abuse",
            "Chat Spam", "Ruining Gameplay", "Threats", "Advertise", "Filter Bypass",
            "Suicide Encouragement", "Hackusation", "Stalking"
    };
    public static final String separator = ", ";

    public static void menu(SpartanPlayer p, SpartanPlayer t) {
        if (p.equals(t)) {
            p.sendMessage(ConfigUtils.replaceWithSyntax(p, Config.messages.getColorfulString("failed_command"), null));
            return;
        }
        int counter = 0;
        int size = 54;
        Inventory inv = p.createInventory(size, menu + t.getName());
        String option = Config.settings.getString("Punishments.report_reasons");

        for (String reason : (option != null ? option.split(separator) : reasons)) {
            counter++;
            InventoryUtils.add(inv, ChatColor.RED + reason, null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_DYE : Material.getMaterial("INK_SACK"), 1, (short) 8), -1);

            if (counter == size) {
                break;
            }
        }
        p.openInventory(inv);
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title) {
        if (!title.startsWith(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        SpartanPlayer t = SpartanBukkit.getPlayer(title.substring(menu.length()));

        if (t == null) {
            p.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
        } else {
            Moderation.report(p, t, item);
            p.sendInventoryCloseMessage(null);
        }
        return true;
    }

    public static List<PlayerReport> getList() {
        return ResearchEngine.getReports(null, 7, true);
    }
}
