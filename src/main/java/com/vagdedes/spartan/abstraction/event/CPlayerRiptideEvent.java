package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CPlayerRiptideEvent {

    public final Player player;
    public final ItemStack item;
    public final Vector velocity;

    public CPlayerRiptideEvent(Player player, ItemStack item, Vector velocity) {
        this.player = player;
        this.item = item;
        this.velocity = velocity;
    }

}
