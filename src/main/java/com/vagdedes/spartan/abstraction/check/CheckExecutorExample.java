package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public class CheckExecutorExample extends CheckExecutor {

    public CheckExecutorExample(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false, false);
        // This is the constructor you will call to initiate this abstract class
        // implementation. If your check/detection has higher complexity, it will
        // likely need to be produced in multiple classes. In that case, you can
        // separate the functionality by using the 'DetectionExecutor' class and
        // connect them all via the 'CheckExecutor' class.
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
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
    protected void cannotHandle(boolean cancelled, Object object) {
        // This method will be called when the 'handleInternal' method cannot run.
        // Reasons for the method being unable to run can vary, such as the check
        // being disabled, the player being in a certain game mode, a compatibility
        // blocking the check, etc.
    }

    @Override
    protected void runInternal(boolean cancelled) {
        // This method should be used to run a check/detection when no information
        // needs to be inserted via the method being called and is all available in
        // the class or via methods of other classes.
        //
        // The boolean 'cancelled' works the same as in the 'handleInternal' method
        // which is where you can find its documentation.
    }

    @Override
    protected void cannotRun(boolean cancelled) {
        // This method will be called when the 'runInternal' method cannot run.
        // Reasons for the method being unable to run can vary, such as the check
        // being disabled, the player being in a certain game mode, a compatibility
        // blocking the check, etc.
    }

    @Override
    protected void scheduler() {
        // Checks/detections support scheduling. This method is called every
        // 1 tick which is worth approximately 50 milliseconds. This method runs
        // as the server runs, so it is called approximately 20 times per second.
        // Keep in mind that based on a player's latency the server may process
        // none to multiple events per tick, meaning this method is prone to
        // information loss if not used correctly.
    }

    @Override
    protected void cannotSchedule() {
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
        return true;
    }

    // Here you can add more methods since you are extending an abstract class.
    // It is nonetheless recommended to stick to the default methods, otherwise
    // you may run into scenarios where you need to use casting to access methods
    // of the child class from the parent class which produces overhead. For
    // comparison, accessing a parent class from a child class is significantly
    // lighter.
}
