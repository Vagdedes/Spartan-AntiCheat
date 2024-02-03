package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.handlers.tracking.CombatProcessing;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class PlayerOpponent {

    private final String name;
    private int hits, damage, maxCPS, hitCombo, maxHitCombo;
    private long lastHit, previousLastHit, lastDamage, hitTimeAverage, duration;
    private double reachAverage, yawRateAverage, pitchRateAverage, cpsAverage;
    private final long start;

    // Separator

    public PlayerOpponent(String name, int hits, int maxHitCombo, int maxCPS, double cpsAverage, long duration, double reachAverage, long hitTimeAverage,
                          double yawRateAverage, double pitchRateAverage) { // Used for logs
        this.name = name;
        this.hits = hits;
        this.damage = 0;
        this.maxCPS = maxCPS;
        this.maxHitCombo = maxHitCombo;

        this.cpsAverage = -cpsAverage;
        this.reachAverage = -reachAverage;
        this.hitTimeAverage = -hitTimeAverage;
        this.yawRateAverage = -yawRateAverage;
        this.pitchRateAverage = -pitchRateAverage;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = 0L;
        this.previousLastHit = 0L;
        this.start = 0L;
        this.duration = duration;
    }

    PlayerOpponent(String name) { // Used for memory
        long time = System.currentTimeMillis();

        this.name = name;
        this.hits = 0;
        this.damage = 0;
        this.maxCPS = 1;
        this.maxHitCombo = 0;

        this.cpsAverage = 0;
        this.reachAverage = 0;
        this.hitTimeAverage = 0;
        this.yawRateAverage = 0;
        this.pitchRateAverage = 0;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = time;
        this.previousLastHit = time;
        this.start = time;
        this.duration = 0L;
    }

    public PlayerOpponent(SpartanPlayer player) { // Used for memory
        this(player.getName());
    }

    // Separator

    public String getName() {
        return name;
    }

    public int getHits() {
        return hits;
    }

    public int getMaxCPS() {
        return maxCPS;
    }

    public long getDuration() {
        return duration;
    }

    public int getMaxHitCombo() {
        return maxHitCombo;
    }

    public boolean hasHitCombo(int number) {
        return hitCombo >= number;
    }

    public boolean hasHitCombo() {
        return hasHitCombo(2);
    }

    public int getCurrentHitCombo() {
        return hitCombo;
    }

    // Separator

    public double getHitsAverage() {
        return hits / (double) duration;
    }

    public long getHitTimeAverage() {
        return hitTimeAverage < 0L ? Math.abs(hitTimeAverage) : hitTimeAverage / hits;
    }

    public double getReachAverage() {
        return reachAverage < 0.0 ? Math.abs(reachAverage) : reachAverage / (double) hits;
    }

    public double getCPSAverage() {
        return cpsAverage < 0.0 ? Math.abs(cpsAverage) : cpsAverage / (double) (hits + damage);
    }

    public double getYawRateAverage() {
        return yawRateAverage < 0.0 ? Math.abs(yawRateAverage) : yawRateAverage / (double) (hits + damage);
    }

    public double getPitchRateAverage() {
        return pitchRateAverage < 0.0 ? Math.abs(pitchRateAverage) : pitchRateAverage / (double) (hits + damage);
    }

    // Separator

    private void updateData(SpartanPlayer p, boolean lastDamage) {
        int cps = p.getClicks().getCount();

        if (cps > this.maxCPS) {
            this.maxCPS = cps;
        }
        this.cpsAverage += cps;
        this.yawRateAverage += CombatProcessing.getDecimal(p, CombatProcessing.yawDifference, 0.0);
        this.pitchRateAverage += CombatProcessing.getDecimal(p, CombatProcessing.pitchDifference, 0.0);
        this.maxHitCombo = Math.max(maxHitCombo, hitCombo);

        if (lastDamage) {
            this.damage++;
            this.lastDamage = System.currentTimeMillis();
            this.hitCombo = 0;
        }
    }

    public void updateData(SpartanPlayer p) {
        updateData(p, true);
    }

    public void increaseHits(SpartanPlayer p, double distance) {
        if (!hasEnded()) { // Asynchronous Protection
            long lastHit = getLastHit();
            this.previousLastHit = lastHit;
            this.lastHit = System.currentTimeMillis();

            this.hits++;
            this.hitCombo++;

            this.reachAverage += distance;
            this.hitTimeAverage += lastHit;
            updateData(p, false);
        }
    }

    // Separator

    public long getLastHit(boolean maxIfNull) {
        return this.lastHit == 0L ? (maxIfNull ? Long.MAX_VALUE : 0L) : System.currentTimeMillis() - this.lastHit;
    }

    public long getLastHit() {
        return getLastHit(true);
    }

    public long getPreviousLastHit(boolean maxIfNull) { // Replacement to 'getLastHit' in situations where data is just updated
        return this.previousLastHit == 0L ? (maxIfNull ? Long.MAX_VALUE : 0L) : System.currentTimeMillis() - this.previousLastHit;
    }

    public long getPreviousLastHit() { // Replacement to 'getLastHit' in situations where data is just updated
        return getPreviousLastHit(true);
    }

    public long getLastDamage() {
        return System.currentTimeMillis() - this.lastDamage;
    }

    boolean hasEnded() {
        return getLastHit() > PlayerFight.maxFightTime || duration != 0L;
    }

    // Separator

    void end(long time) {
        this.duration = time - start;
    }

}
