package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.Location;

public class CheckBoundData {
    private SpartanProtocol attacker;
    private Location target;
    public int failures;

    public CheckBoundData(SpartanProtocol player, SpartanProtocol target) {
        this.attacker = player;
        this.target = target.getLocation();
        this.failures = 0;
    }

    public CheckBoundData(SpartanProtocol player, Location target) {
        this.attacker = player;
        this.target = target;
        this.failures = 0;
    }

    public SpartanProtocol getAttacker() {
        return this.attacker;
    }

    public Location getTarget() {
        return this.target;
    }

    public int getFailures() {
        return this.failures;
    }

    public void setAttacker(SpartanProtocol attacker) {
        this.attacker = attacker;
    }

    public void setTarget(Location target) {
        this.target = target;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CheckBoundData)) return false;
        final CheckBoundData other = (CheckBoundData) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$attacker = this.getAttacker();
        final Object other$attacker = other.getAttacker();
        if (this$attacker == null ? other$attacker != null : !this$attacker.equals(other$attacker)) return false;
        final Object this$target = this.getTarget();
        final Object other$target = other.getTarget();
        if (this$target == null ? other$target != null : !this$target.equals(other$target)) return false;
        if (this.getFailures() != other.getFailures()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CheckBoundData;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $attacker = this.getAttacker();
        result = result * PRIME + ($attacker == null ? 43 : $attacker.hashCode());
        final Object $target = this.getTarget();
        result = result * PRIME + ($target == null ? 43 : $target.hashCode());
        result = result * PRIME + this.getFailures();
        return result;
    }

    public String toString() {
        return "CheckBoundData(attacker=" + this.getAttacker() + ", target=" + this.getTarget() + ", failures=" + this.getFailures() + ")";
    }
}
