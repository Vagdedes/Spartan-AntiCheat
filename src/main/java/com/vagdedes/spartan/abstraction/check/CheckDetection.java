package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckDetection {

    public final CheckExecutor executor;
    public final Enums.HackType hackType;
    public final SpartanPlayer player;

    // Check
    protected CheckDetection(Enums.HackType hackType, SpartanPlayer player) {
        this.executor = (CheckExecutor) this;
        this.hackType = hackType;
        this.player = player;
    }

    // Detection
    protected CheckDetection(CheckExecutor executor) {
        this.executor = executor;
        this.hackType = executor.hackType;
        this.player = executor.player;
    }

}
