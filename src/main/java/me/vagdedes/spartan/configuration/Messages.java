package me.vagdedes.spartan.configuration;

import me.vagdedes.spartan.abstraction.ConfigurationBuilder;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.utils.server.ConfigUtils;

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
        ConfigUtils.add(file, "console_name", "Console");
        ConfigUtils.add(file, "no_permission", "&cYou don't have permission to interact with this.");
        ConfigUtils.add(file, "player_not_found_message", "&cPlayer not found.");
        ConfigUtils.add(file, "staff_chat_message", "&8[&4Staff Chat&8]&c {player} &e{message}");
        ConfigUtils.add(file, "not_enough_saved_logs", "&8[&2" + prefix + "&8]&c Not enough saved logs. Must be at least {amount} rows.");

        // Config
        ConfigUtils.add(file, "config_reload", "&8[&2" + prefix + "&8]&e Config successfully reloaded.");

        // Kick
        ConfigUtils.add(file, "kick_reason", "&c {reason}");
        ConfigUtils.add(file, "kick_broadcast_message", "&8[&2" + prefix + "&8]&c {player}&7 was kicked for&4 {reason}");
        ConfigUtils.add(file, "reconnect_kick_message", "&8[&2" + prefix + "&8]&c Please wait a few seconds before logging back in.");
        ConfigUtils.add(file, "player_ip_limit_kick_message", "&8[&2" + prefix + "&8]&c The player limit of your IP has been reached.");

        // Violations
        ConfigUtils.add(file, "check_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for check&8: &2{check}");
        ConfigUtils.add(file, "player_violation_reset_message", "&8[&2" + prefix + "&8]&a Violations successfully reset for player&8: &2{player}");
        ConfigUtils.add(file, "player_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for player&8: &2{player}");
        ConfigUtils.add(file, "detection_notification", "&8[&2" + prefix + "&8] "
                + "&4{player} &cis &4{detection:category:adverb} &cusing &4{detection} x{vls:detection} "
                + "&8&b| &r&f{ping}ms &8&b| &r&f{tps} TPS &8&b| &r&fsilent: {silent:detection}, {info}");

        // Chat Protection
        ConfigUtils.add(file, "blocked_command_message", "&8[&2" + prefix + "&8]&c You are not allowed to dispatch that command.");
        ConfigUtils.add(file, "blocked_word_message", "&8[&2" + prefix + "&8]&c You are not allowed to type that.");
        ConfigUtils.add(file, "chat_cooldown_message", "&8[&2" + prefix + "&8]&c Please wait {time} second(s) until typing again.");
        ConfigUtils.add(file, "command_cooldown_message", "&8[&2" + prefix + "&8]&c Please wait {time} second(s) until dispatching a command again.");
        ConfigUtils.add(file, "same_message_warning", "&8[&2" + prefix + "&8]&c Please avoid sending the same message again.");

        // Checks
        ConfigUtils.add(file, "check_enable_message", "&8[&2" + prefix + "&8] &aYou enabled the check&8:&7 {detection}");
        ConfigUtils.add(file, "check_disable_message", "&8[&2" + prefix + "&8] &cYou disabled the check&8:&7 {detection}");
        ConfigUtils.add(file, "non_existing_check", "&8[&2" + prefix + "&8] &cThis check doesn't exist.");
        ConfigUtils.add(file, "bypass_message", "&8[&2" + prefix + "&8] &c{player} &7is now bypassing the &4{detection} &7check for &e{time} &7seconds.");

        // Warnings
        ConfigUtils.add(file, "warning_message", "&c {reason}");
        ConfigUtils.add(file, "warning_feedback_message", "&8[&2" + prefix + "&8]&7 You warned &c{player} &7for&8: &4{reason}");

        // Bans
        ConfigUtils.add(file, "ban_message", "&8[&2" + prefix + "&8]&7 You banned &c{player} &7for &4{reason}");
        ConfigUtils.add(file, "unban_message", "&8[&2" + prefix + "&8]&7 You unbanned &c{player}");
        ConfigUtils.add(file, "player_not_banned", "&8[&2" + prefix + "&8]&c This player is not banned.");
        ConfigUtils.add(file, "ban_broadcast_message", "&8[&2" + prefix + "&8]&c {player}&7 was banned for&4 {reason}");
        ConfigUtils.add(file, "ban_reason", "&c {reason}");
        ConfigUtils.add(file, "empty_ban_list", "&8[&2" + prefix + "&8]&c There are currently no banned players.");

        // Notifications
        ConfigUtils.add(file, "notifications_enable", "&8[&2" + prefix + "&8] &aYou enabled notifications.");
        ConfigUtils.add(file, "notifications_modified", "&8[&2" + prefix + "&8] &eYou modified notifications.");
        ConfigUtils.add(file, "notifications_disable", "&8[&2" + prefix + "&8] &cYou disabled notifications.");
        ConfigUtils.add(file, "awareness_notification", "&8[&2" + prefix + " Notification&8]&a {info}");
        ConfigUtils.add(file, "suspicion_notification", "&8[&2" + prefix + "&8] &6{size} player(s) &emay be using hack modules&8: &6{players}" +
                "\n&eRun '/spartan menu' to view such future interactions.");

        // Waves
        ConfigUtils.add(file, "wave_start_message", "&8[&2" + prefix + "&8]&c The wave is starting.");
        ConfigUtils.add(file, "wave_end_message", "&8[&2" + prefix + "&8]&c The wave has ended with a total of {total} action(s).");
        ConfigUtils.add(file, "wave_clear_message", "&8[&2" + prefix + "&8]&c The wave was cleared.");
        ConfigUtils.add(file, "wave_add_message", "&8[&2" + prefix + "&8]&a {player} was added to the wave.");
        ConfigUtils.add(file, "wave_remove_message", "&8[&2" + prefix + "&8]&c {player} was removed from the wave.");
        ConfigUtils.add(file, "full_wave_list", "&8[&2" + prefix + "&8]&c The wave list is full.");
        ConfigUtils.add(file, "wave_not_added_message", "&8[&2" + prefix + "&8]&c {player} is not added to the wave.");

        // Reports
        ConfigUtils.add(file, "report_message", "&8[&2" + prefix + "&8] &a{player} &7reported &c{reported} &7for&8: &4{reason}");
        ConfigUtils.add(file, "self_report_message", "&8[&2" + prefix + "&8]&c You can't report yourself.");

        // Debug
        ConfigUtils.add(file, "debug_player_message", "&8[&2" + prefix + "&8]&7 Debugging &6{player}&7's &e{type}&8: &f{info}");
        ConfigUtils.add(file, "debug_enable_message", "&8[&2" + prefix + "&8]&7 Enabled debugging for &6{player}&7's &e{type}");
        ConfigUtils.add(file, "debug_disable_message", "&8[&2" + prefix + "&8]&7 Disabled debugging for &6{player}&7's &e{type}");
        ConfigUtils.add(file, "debug_disable_all_message", "&8[&2" + prefix + "&8]&7 Disabled debugging for &6{player}");

        // Commands
        ConfigUtils.add(file, "unknown_command", "&fUnknown command. Please type \"/help\" for help.");
        ConfigUtils.add(file, "failed_command", "&8[&2" + prefix + "&8]&c Command failed. Please check your arguments and try again.");
        ConfigUtils.add(file, "successful_command", "&8[&2" + prefix + "&8]&a Command successful.");
        ConfigUtils.add(file, "massive_command_reason", "&8[&2" + prefix + "&8]&c The length of the reason is too big.");

        // Spectating
        ConfigUtils.add(file, "spectating_player", "&8[&2" + prefix + "&8]&a You are now spectating {player}, type '/spectate' to exit.");
        ConfigUtils.add(file, "spectating_ended", "&8[&2" + prefix + "&8]&c You are no longer spectating someone.");

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }
}
