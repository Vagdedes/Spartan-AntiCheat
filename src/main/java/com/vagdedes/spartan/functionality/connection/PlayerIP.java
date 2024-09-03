package com.vagdedes.spartan.functionality.connection;

import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class PlayerIP {

    public static String get(Player p) {
        InetSocketAddress ip = p.getAddress();

        if (ip != null) {
            InetAddress address = ip.getAddress();
            return address == null ? null : get(address);
        }
        return null;
    }

    public static String get(InetAddress address) {
        return address.toString().substring(1);
    }

}
