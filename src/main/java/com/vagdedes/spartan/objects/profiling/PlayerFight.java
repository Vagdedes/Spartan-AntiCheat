package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public class PlayerFight {

    final PlayerOpponent opponent1, opponent2;
    private long duration;
    private boolean judged;

    PlayerFight(PlayerOpponent opponent1, PlayerOpponent opponent2) { // Used for memory
        this.opponent1 = opponent1;
        this.opponent2 = opponent2;
        this.judged = false;
        this.duration = System.currentTimeMillis();
    }

    public PlayerOpponent[] getOpponent(SpartanPlayer player) {
        return opponent1.player.equals(player) ?
                new PlayerOpponent[]{opponent1, opponent2} :
                new PlayerOpponent[]{opponent2, opponent1};
    }

    PlayerOpponent[] getOpponent(PlayerProfile profile) {
        return opponent1.player.getProfile().equals(profile) ?
                new PlayerOpponent[]{opponent1, opponent2} :
                new PlayerOpponent[]{opponent2, opponent1};
    }

    public double getHitRatio(SpartanPlayer player) {
        PlayerOpponent[] opponents = getOpponent(player);
        int targetHits = opponents[1].getHits();
        return targetHits == 0
                ? opponents[0].getHits()
                : (opponents[0].getHits() / ((double) targetHits));
    }

    public boolean isKill() {
        return !judged;
    }

    public long getDuration() {
        return duration;
    }

    public void setWinner(SpartanPlayer opponent) { // Use only when player is in said fight
        finish(getOpponent(opponent)[0]);
        opponent1.player.getProfile().getCombat().removeFight(this);
        opponent2.player.getProfile().getCombat().removeFight(this);
    }

    public void update(SpartanPlayer damager) {
        PlayerOpponent[] opponents = getOpponent(damager);
        opponents[0].damager();
        opponents[1].damaged();
    }

    boolean expired() {
        if (Math.min(
                opponent1.getLastHit(false),
                opponent2.getLastHit(false)
        ) > 20_000L) {
            this.judged = true;

            if (opponent1.getHits() > opponent2.getHits()) {
                finish(opponent1);
            } else if (opponent2.getHits() > opponent1.getHits()) {
                finish(opponent2);
            }
            return true;
        }
        return false;
    }

    private void finish(PlayerOpponent winner) {
        this.duration = System.currentTimeMillis() - this.duration;
        winner.player.getExecutor(Enums.HackType.KillAura).handle(false, this);
    }
}
