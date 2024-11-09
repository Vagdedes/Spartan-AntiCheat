package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckDetection {

    public final CheckExecutor executor;
    public final Enums.HackType hackType;
    SpartanProtocol protocol;
    private final Object synchronizer;

    // Check
    protected CheckDetection(Enums.HackType hackType, SpartanProtocol protocol) {
        this.executor = (CheckExecutor) this;
        this.hackType = hackType;
        this.protocol = protocol;
        this.synchronizer = new Object();
    }

    // Detection
    protected CheckDetection(CheckExecutor executor) {
        this.executor = executor;
        this.hackType = executor.hackType;
        this.protocol = executor.protocol;
        this.synchronizer = new Object();
    }

    public final SpartanProtocol protocol() {
        synchronized (this.synchronizer) {
            return this.protocol;
        }
    }

    public final void setProtocol(SpartanProtocol protocol) {
        synchronized (this.synchronizer) {
            this.protocol = protocol;
        }
    }

}
