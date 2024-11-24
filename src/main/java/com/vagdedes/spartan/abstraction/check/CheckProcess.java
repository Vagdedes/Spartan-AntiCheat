package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckProcess {

    public final Enums.HackType hackType;
    SpartanProtocol protocol;
    private final Object synchronizer;

    protected CheckProcess(Enums.HackType hackType, SpartanProtocol protocol) {
        this.hackType = hackType;
        this.protocol = protocol;
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
