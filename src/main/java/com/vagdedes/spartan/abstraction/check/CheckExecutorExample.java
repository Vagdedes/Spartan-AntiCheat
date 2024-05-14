package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;

public class CheckExecutorExample extends CheckExecutor {

    // This is the list of all checks/detections that are implemented in the plugin's code.
    // The developer has not made the following classes available to you, so you should
    // remove their paths from the list and add the paths of the classes you may implement.
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

    public CheckExecutorExample(SpartanPlayer player) {
        super(null, player);
        // This is the constructor you will call to initiate this abstract class
        // implementation. If your check/detection has higher complexity, it will
        // likely need to be produced in multiple classes. In that case, you can
        // separate the functionality by using the 'DetectionExecutor' class and
        // connect them all via the 'CheckExecutor' class.
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        // This method should be used to handle data for a check/detection when
        // the information is not directly available via the class or other classes.
        // You may also use this method to run checks/detections, although it is best
        // you use the 'runInternal' method for that purpose.
        //
        // The boolean 'cancelled' is 'true' when an event is cancelled by the server
        // or by another plugin. Based on configuration, a user of this plugin may
        // choose for cancelled events to not go through, thus causing this method to
        // not be called at all.
    }

    @Override
    public void cannotHandle(boolean cancelled, Object object) {
        // This method will be called when the 'handleInternal' method cannot run.
        // Reasons for the method being unable to run can vary, such as the check
        // being disabled, the player being in a certain game mode, a compatibility
        // blocking the check, etc.
    }

    @Override
    public void runInternal(boolean cancelled) {
        // This method should be used to run a check/detection when no information
        // needs to be inserted via the method being called and is all available in
        // the class or via methods of other classes.
        //
        // The boolean 'cancelled' works the same as in the 'handleInternal' method
        // which is where you can find its documentation.
    }

    @Override
    public void cannotRun(boolean cancelled) {
        // This method will be called when the 'runInternal' method cannot run.
        // Reasons for the method being unable to run can vary, such as the check
        // being disabled, the player being in a certain game mode, a compatibility
        // blocking the check, etc.
    }

    @Override
    public void schedulerInternal() {
        // Checks/detections support scheduling. This method is called every
        // 1 tick which is worth approximately 50 milliseconds. This method runs
        // as the server runs, so it is called approximately 20 times per second.
        // Keep in mind that based on a player's latency the server may process
        // none to multiple events per tick, meaning this method is prone to
        // information loss if not used correctly.
    }

    @Override
    public void cannotSchedule() {
        // This method will be called when the 'schedulerInternal' method cannot run.
        // Reasons for the method being unable to run can vary, such as the check
        // being disabled, the player being in a certain game mode, a compatibility
        // blocking the check, etc.
    }

    @Override
    protected boolean canRun() {
        // This method should be used to judge whether a check should run or not.
        // However, each check/detection may have different requirements, so use
        // this method for the requirements all checks/detections have in common.
        // Keep in mind that basic factors such as the check being enabled are
        // already accounted for prior to running this method.
        return false;
    }

    // Here you can add more methods since you are extending an abstract class.
    // It is nonetheless recommended to stick to the default methods, otherwise
    // you may run into scenarios where you need to use casting to access methods
    // of the child class from the parent class which produces overhead. For
    // comparison, accessing a parent class from a child class is significantly
    // lighter.
}
