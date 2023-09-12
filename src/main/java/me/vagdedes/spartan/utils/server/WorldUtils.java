package me.vagdedes.spartan.utils.server;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldUtils {

    public static int getID(World w) {
        int i = -1;

        for (World world : Bukkit.getWorlds()) {
            i++;

            if (w.equals(world)) {
                break;
            }
        }
        return i;
    }

    public static World getByID(int id) {
        int i = -1;

        for (World world : Bukkit.getWorlds()) {
            i++;

            if (i == id) {
                return world;
            }
        }
        return null;
    }
}
