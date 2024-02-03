package com.vagdedes.spartan.functionality.moderation;

import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.Moderation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerReport;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerReports extends InventoryMenu {

    private static final String menu = "Report: ";
    public static final String[] reasons = new String[]{
            "Hacking", "Offensive Comments", "Bad Sportsmanship", "Mechanism Abuse",
            "Chat Spam", "Ruining Gameplay", "Threats", "Advertise", "Filter Bypass",
            "Suicide Encouragement", "Hackusation", "Stalking"
    };
    public static final String separator = ", ";

    public PlayerReports() {
        super(menu, 54, new Enums.Permission[]{});
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        SpartanPlayer target = (SpartanPlayer) object;

        if (player.equals(target)) {
            player.sendInventoryCloseMessage(ConfigUtils.replaceWithSyntax(player, Config.messages.getColorfulString("failed_command"), null));
            return false;
        } else {
            int counter = 0;
            int size = 54;
            setTitle(player, menu + target.getName());
            String option = Config.settings.getString("Punishments.report_reasons");

            for (String reason : (option != null ? option.split(separator) : reasons)) {
                counter++;
                InventoryUtils.add(inventory, ChatColor.RED + reason, null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_DYE : Material.getMaterial("INK_SACK"), 1, (short) 8), -1);

                if (counter == size) {
                    break;
                }
            }
            return true;
        }
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        SpartanPlayer t = SpartanBukkit.getPlayer(title.substring(menu.length()));

        if (t == null) {
            player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
        } else {
            Moderation.report(player, t, item);
            player.sendInventoryCloseMessage(null);
        }
        return true;
    }

    public List<PlayerReport> getList() {
        return ResearchEngine.getReports(null, 7, true);
    }
}
