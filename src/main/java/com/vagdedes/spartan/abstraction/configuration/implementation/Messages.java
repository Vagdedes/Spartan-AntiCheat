package com.vagdedes.spartan.abstraction.configuration.implementation;

import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;

public class Messages extends ConfigurationBuilder {

    public Messages() {
        super("messages");
    }

    @Override
    public void create() {
        addOption("console_name", "Console");
        addOption("no_permission", "&8[&2" + prefix + "&8]&c You don't have permission to interact with this.");
        addOption("player_not_found_message", "&8[&2" + prefix + "&8]&c Player not found.");

        // Config
        addOption("config_reload", "&8[&2" + prefix + "&8]&e Config successfully reloaded.");

        // Panic
        addOption("panic_mode_enable", "&8[&2" + prefix + "&8]&a Panic mode enabled, all checks are set to silent and will not punish players.");
        addOption("panic_mode_disable", "&8[&2" + prefix + "&8]&c Panic mode disabled, all checks will now run per configuration standards.");

        // Kick
        addOption("kick_reason", "&c {reason}");
        addOption("kick_broadcast_message", "&8[&2" + prefix + "&8]&c {player}&7 was kicked for&4 {reason}");
        addOption("player_ip_limit_kick_message", "&8[&2" + prefix + "&8]&c The player limit of your IP has been reached.");

        // Violations
        addOption("player_violation_reset_message", "&8[&2" + prefix + "&8]&a Violations successfully reset for player&8: &2{player}");
        addOption("player_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for player&8: &2{player}");
        addOption("detection_notification", "&8[&2" + prefix + "&8] "
                + "&e{player} &7>> &c{detection:real}§8[§4{detection:percentage}§8] &7(§f{info}§7)");

        // Checks
        addOption("check_stored_data_delete_message", "&8[&2" + prefix + "&8]&a Stored data successfully deleted for check&8: &2{check}");
        addOption("check_enable_message", "&8[&2" + prefix + "&8] &aYou enabled the check&8:&7 {detection}");
        addOption("check_disable_message", "&8[&2" + prefix + "&8] &cYou disabled the check&8:&7 {detection}");
        addOption("check_silent_disable_message", "&8[&2" + prefix + "&8] &aYou enabled preventions for the check&8:&7 {detection}");
        addOption("check_silent_enable_message", "&8[&2" + prefix + "&8] &cYou disabled preventions for the check&8:&7 {detection}");
        addOption("check_punishment_enable_message", "&8[&2" + prefix + "&8] &aYou enabled punishments for the check&8:&7 {detection}");
        addOption("check_punishment_disable_message", "&8[&2" + prefix + "&8] &cYou disabled punishments for the check&8:&7 {detection}");
        addOption("non_existing_check", "&8[&2" + prefix + "&8] &cThis check doesn't exist.");
        addOption("bypass_message", "&8[&2" + prefix + "&8] &c{player} &7is now bypassing the &4{detection} &7check for &e{time} &7seconds.");

        // Warnings
        addOption("warning_message", "&c {reason}");
        addOption("warning_feedback_message", "&8[&2" + prefix + "&8]&7 You warned &c{player} &7for&8: &4{reason}");

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

        // Commands
        addOption("unknown_command", "&fUnknown command. Please type \"/help\" for help.");
        addOption("failed_command", "&8[&2" + prefix + "&8]&c Command failed ({command}). Please check your arguments and try again.");
        addOption("successful_command", "&8[&2" + prefix + "&8]&a Command successful.");
        addOption("massive_command_reason", "&8[&2" + prefix + "&8]&c The length of the reason is too big.");
    }
}
