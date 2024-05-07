package com.vagdedes.spartan.abstraction.check;

public class EmptyCheckExecutor extends CheckExecutor {

    public static final String[] executors = new String[]{
            "com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.KillAura",
            "com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks.FastClicks",
            "com.vagdedes.spartan.abstraction.check.implementation.combat.Criticals",
            "com.vagdedes.spartan.abstraction.check.implementation.combat.FastBow",
            "com.vagdedes.spartan.abstraction.check.implementation.combat.HitReach",
            "com.vagdedes.spartan.abstraction.check.implementation.combat.Velocity",

            "com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements",
            "com.vagdedes.spartan.abstraction.check.implementation.movement.speed.Speed",
            "com.vagdedes.spartan.abstraction.check.implementation.movement.MorePackets",
            "com.vagdedes.spartan.abstraction.check.implementation.movement.NoFall",

            "com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits",

            "com.vagdedes.spartan.abstraction.check.implementation.inventory.ImpossibleInventory",
            "com.vagdedes.spartan.abstraction.check.implementation.inventory.InventoryClicks",
            "com.vagdedes.spartan.abstraction.check.implementation.inventory.ItemDrops",

            "com.vagdedes.spartan.abstraction.check.implementation.player.AutoRespawn",
            "com.vagdedes.spartan.abstraction.check.implementation.player.FastEat",
            "com.vagdedes.spartan.abstraction.check.implementation.player.FastHeal",
            "com.vagdedes.spartan.abstraction.check.implementation.player.NoSwing",

            "com.vagdedes.spartan.abstraction.check.implementation.world.BlockReach",
            "com.vagdedes.spartan.abstraction.check.implementation.world.FastBreak",
            "com.vagdedes.spartan.abstraction.check.implementation.world.FastPlace",
            "com.vagdedes.spartan.abstraction.check.implementation.world.GhostHand",
            "com.vagdedes.spartan.abstraction.check.implementation.world.ImpossibleActions",
            "com.vagdedes.spartan.abstraction.check.implementation.world.XRay",
    };

    public EmptyCheckExecutor() {
        super(null, null);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return false;
    }
}
