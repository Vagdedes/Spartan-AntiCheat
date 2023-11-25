package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

public class PlayerOpponent {

    private final String name;
    private int hits, maxCPS, hitCombo, maxHitCombo;
    private long lastHit, previousLastHit, lastDamage, hitTimeAverage, duration;
    private double reachAverage;
    private final long start;
    private float yawRateAverage, pitchRateAverage;

    // Separator

    public static int getEntities(SpartanPlayer player) {
        return PlayerData.getEntitiesNumber(player, CombatUtils.maxHitDistance, false);
    }

    // Separator

    public PlayerOpponent(String name, int hits, int maxHitCombo, int maxCPS, long duration, double reachAverage, long hitAverage,
                          float yawRateAverage, float pitchRateAverage) { // Used for logs
        this.name = name;
        this.hits = hits;
        this.maxCPS = maxCPS;

        this.reachAverage = -reachAverage;
        this.hitTimeAverage = -hitAverage;
        this.yawRateAverage = -yawRateAverage;
        this.pitchRateAverage = -pitchRateAverage;
        this.maxHitCombo = -maxHitCombo;

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
        this.maxCPS = 1;

        this.reachAverage = 0;
        this.hitTimeAverage = 0;
        this.yawRateAverage = 0f;
        this.pitchRateAverage = 0f;
        this.maxHitCombo = 0;

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
        return Math.abs(maxHitCombo);
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

    public long getHitTimeAverage() {
        return Math.abs(hitTimeAverage);
    }

    public double getReachAverage() {
        return Math.abs(reachAverage);
    }

    public float getYawRateAverage() {
        return Math.abs(yawRateAverage);
    }

    public float getPitchRateAverage() {
        return Math.abs(pitchRateAverage);
    }

    // Separator

    private void updateData(SpartanPlayer p, int entities, boolean lastDamage) {
        int cps = p.getClicks().getCount();

        if (cps > this.maxCPS) {
            this.maxCPS = cps;
        }
        this.maxHitCombo = Math.max(maxHitCombo, hitCombo);

        if (lastDamage) {
            this.lastDamage = System.currentTimeMillis();
            this.hitCombo = 0;
        }
    }

    public void updateData(SpartanPlayer p, int entities) {
        updateData(p, entities, true);
    }

    public void increaseHits(SpartanPlayer p, double distance, int entities) {
        if (!hasEnded()) { // Asynchronous Protection
            long lastHit = getLastHit();
            hitTimeAverage += lastHit;
            previousLastHit = lastHit;
            this.lastHit = System.currentTimeMillis();

            hits++;
            hitCombo++;
            reachAverage = (reachAverage + distance) / ((double) hits);
            yawRateAverage = (yawRateAverage + ((float) CombatProcessing.getDecimal(p, CombatProcessing.yawDifference, 0.0))) / ((float) hits);
            pitchRateAverage = (pitchRateAverage + ((float) CombatProcessing.getDecimal(p, CombatProcessing.pitchDifference, 0.0))) / ((float) hits);
            updateData(p, entities, false);
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
