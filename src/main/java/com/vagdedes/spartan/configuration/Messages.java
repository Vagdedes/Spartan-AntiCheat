package com.vagdedes.spartan.configuration;

import com.vagdedes.spartan.abstraction.ConfigurationBuilder;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;

import java.io.File;

public class Messages extends ConfigurationBuilder {

    public Messages() {
        super("messages");
    }

    @Override
    public void clear() {
        internalClear();
    }

    @Override
    public void create(boolean local) {
        file = new File(directory);
        boolean exists = file.exists();

        clear();
        addOption("console_name", "Console");
        addOption("no_permission", "&cYou don't have permission to interact with this.");
        addOption("player_not_found_message", "&cPlayer not found.");
        addOption("staff_chat_message", "&8[&4Staff Chat&8]&c {player} &e{message}");
        addOption("not_enough_saved_logs", "&8[&2" + prefix + "&8]&c Not enough saved logs. Must be at least {amount} rows.");

        // Config
        addOption("config_reload", "&8[&2" + prefix + "&8]&e Config successfully reloaded.");

        // Kick
        addOption("kick_reason", "&c {reason}");
        addOption("kick_broadcast_message", "&8[&2" + prefix + "&8]&c {player}&7 was kicked for&4 {reason}");
        addOption("reconnect_kick_message", "&8[&2" + prefix + "&8]&c Please wait a few seconds before logging back in.");
        addOption("player_ip_limit_kick_message", "&8[&2" + prefix + "&8]&c The player limit of your IP has been reached.");

        // Violations
        addOption("check_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for check&8: &2{check}");
        addOption("player_violation_reset_message", "&8[&2" + prefix + "&8]&a Violations successfully reset for player&8: &2{player}");
        addOption("player_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for player&8: &2{player}");
        addOption("detection_notification", "&8[&2" + prefix + "&8] "
                + "&4{player} &cis &4{detection:category:adverb} &cusing &4{detection} x{vls:detection} "
                + "&8&b| &r&f{ping}ms &8&b| &r&f{tps} TPS &8&b| &r&fsilent: {silent:detection}, {info}");

        // Chat Protection
        addOption("blocked_command_message", "&8[&2" + prefix + "&8]&c You are not allowed to dispatch that command.");
        addOption("blocked_word_message", "&8[&2" + prefix + "&8]&c You are not allowed to type that.");
        addOption("chat_cooldown_message", "&8[&2" + prefix + "&8]&c Please wait {time} second(s) until typing again.");
        addOption("command_cooldown_message", "&8[&2" + prefix + "&8]&c Please wait {time} second(s) until dispatching a command again.");
        addOption("same_message_warning", "&8[&2" + prefix + "&8]&c Please avoid sending the same message again.");

        // Checks
        addOption("check_enable_message", "&8[&2" + prefix + "&8] &aYou enabled the check&8:&7 {detection}");
        addOption("check_disable_message", "&8[&2" + prefix + "&8] &cYou disabled the check&8:&7 {detection}");
        addOption("non_existing_check", "&8[&2" + prefix + "&8] &cThis check doesn't exist.");
        addOption("bypass_message", "&8[&2" + prefix + "&8] &c{player} &7is now bypassing the &4{detection} &7check for &e{time} &7seconds.");

        // Warnings
        addOption("warning_message", "&c {reason}");
        addOption("warning_feedback_message", "&8[&2" + prefix + "&8]&7 You warned &c{player} &7for&8: &4{reason}");

        // Bans
        addOption("ban_message", "&8[&2" + prefix + "&8]&7 You banned &c{player} &7for &4{reason}");
        addOption("unban_message", "&8[&2" + prefix + "&8]&7 You unbanned &c{player}");
        addOption("player_not_banned", "&8[&2" + prefix + "&8]&c This player is not banned.");
        addOption("ban_broadcast_message", "&8[&2" + prefix + "&8]&c {player}&7 was banned for&4 {reason}");
        addOption("ban_reason", "&c {reason}");
        addOption("empty_ban_list", "&8[&2" + prefix + "&8]&c There are currently no banned players.");

        // Notifications
        addOption("notifications_enable", "&8[&2" + prefix + "&8] &aYou enabled notifications.");
        addOption("notifications_modified", "&8[&2" + prefix + "&8] &eYou modified notifications.");
        addOption("notifications_disable", "&8[&2" + prefix + "&8] &cYou disabled notifications.");
        addOption("awareness_notification", "&8[&2" + prefix + " Notification&8]&a {info}");
        addOption("suspicion_notification", "&8[&2" + prefix + "&8] &6{size} player(s) &emay be using hack modules&8: &6{players}" +
                "\n&eRun '/spartan menu' to view such future interactions.");

        // Waves
        addOption("wave_start_message", "&8[&2" + prefix + "&8]&c The wave is starting.");
        addOption("wave_end_message", "&8[&2" + prefix + "&8]&c The wave has ended with a total of {total} action(s).");
        addOption("wave_clear_message", "&8[&2" + prefix + "&8]&c The wave was cleared.");
        addOption("wave_add_message", "&8[&2" + prefix + "&8]&a {player} was added to the wave.");
        addOption("wave_remove_message", "&8[&2" + prefix + "&8]&c {player} was removed from the wave.");
        addOption("full_wave_list", "&8[&2" + prefix + "&8]&c The wave list is full.");
        addOption("wave_not_added_message", "&8[&2" + prefix + "&8]&c {player} is not added to the wave.");

        // Reports
        addOption("report_message", "&8[&2" + prefix + "&8] &a{player} &7reported &c{reported} &7for&8: &4{reason}");
        addOption("self_report_message", "&8[&2" + prefix + "&8]&c You can't report yourself.");

        // Debug
        addOption("debug_player_message", "&8[&2" + prefix + "&8]&7 Debugging &6{player}&7's &e{type}&8: &f{info}");
        addOption("debug_enable_message", "&8[&2" + prefix + "&8]&7 Enabled debugging for &6{player}&7's &e{type}");
        addOption("debug_disable_message", "&8[&2" + prefix + "&8]&7 Disabled debugging for &6{player}&7's &e{type}");
        addOption("debug_disable_all_message", "&8[&2" + prefix + "&8]&7 Disabled debugging for &6{player}");

        // Commands
        addOption("unknown_command", "&fUnknown command. Please type \"/help\" for help.");
        addOption("failed_command", "&8[&2" + prefix + "&8]&c Command failed. Please check your arguments and try again.");
        addOption("successful_command", "&8[&2" + prefix + "&8]&a Command successful.");
        addOption("massive_command_reason", "&8[&2" + prefix + "&8]&c The length of the reason is too big.");

        // Spectating
        addOption("spectating_player", "&8[&2" + prefix + "&8]&a You are now spectating {player}, type '/spectate' to exit.");
        addOption("spectating_ended", "&8[&2" + prefix + "&8]&c You are no longer spectating someone.");

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }
}
