package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;

public class PlayerOpponent {

    public final SpartanPlayer player;
    private int hits, damage, hitCombo;
    private long lastHit, lastDamage;

    // Separator

    PlayerOpponent(SpartanPlayer player) { // Used for memory
        this.player = player;
        this.hits = 0;
        this.damage = 0;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = 0L;
    }

    public int getHits() {
        return hits;
    }

    public int getDamage() {
        return damage;
    }

    public boolean hasHitCombo() {
        return hitCombo >= 2;
    }

    public int getHitCombo() {
        return hitCombo;
    }

    void damaged() {
        this.damage++;
        this.lastDamage = System.currentTimeMillis();
        this.hitCombo = 0;
    }

    void damager() {
        this.lastHit = System.currentTimeMillis();

        this.hits++;
        this.hitCombo++;
    }

    public long getLastHit(boolean maxIfNull) {
        return this.lastHit == 0L
                ? (maxIfNull ? Long.MAX_VALUE : 0L)
                : System.currentTimeMillis() - this.lastHit;
    }

    public long getLastDamage(boolean maxIfNull) {
        return this.lastDamage == 0L
                ? (maxIfNull ? Long.MAX_VALUE : 0L)
                : System.currentTimeMillis() - this.lastDamage;
    }

}
