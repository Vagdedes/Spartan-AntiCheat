package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckProcess {

    public final Enums.HackType hackType;
    private SpartanProtocol protocol;

    protected CheckProcess(Enums.HackType hackType, SpartanProtocol protocol) {
        this.hackType = hackType;
        this.protocol = protocol;
    }

    public final SpartanProtocol protocol() {
        return this.protocol;
    }

    public final void setProtocol(SpartanProtocol protocol) {
        this.protocol = protocol;
    }

}
