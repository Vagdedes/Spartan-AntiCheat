package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckProcess {

    public final Enums.HackType hackType;
    private SpartanProtocol protocol;
    private String playerName;

    protected CheckProcess(Enums.HackType hackType, SpartanProtocol protocol, String playerName) {
        this.hackType = hackType;
        this.protocol = protocol;
        this.playerName = protocol == null ? playerName : protocol.bukkit.getName();
    }

    public final SpartanProtocol protocol() {
        return this.protocol;
    }

    protected final String playerName() {
        return this.playerName;
    }

    public final PlayerProfile profile() {
        if (this.protocol != null) {
            return this.protocol.profile();
        } else {
            return ResearchEngine.getPlayerProfile(this.playerName, true);
        }
    }

    public final void setProtocol(SpartanProtocol protocol) {
        this.protocol = protocol;

        if (this.protocol != null) {
            this.playerName = this.protocol.bukkit.getName();
        }
    }

}
