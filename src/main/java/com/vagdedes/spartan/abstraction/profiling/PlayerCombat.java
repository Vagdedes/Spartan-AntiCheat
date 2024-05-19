package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class PlayerCombat {

    // Separator

    private final PlayerProfile profile;

    // Use of memory based lists to help save performance when iterating in the object (no advantage when iterating globally)
    final Collection<PlayerFight> fights;

    PlayerCombat(PlayerProfile profile) {
        this.profile = profile;
        this.fights = Collections.synchronizedList(new ArrayList<>());
    }

    public boolean isFighting() {
        int size = fights.size();

        if (size > 0) {
            synchronized (fights) {
                Iterator<PlayerFight> iterator = fights.iterator();

                while (iterator.hasNext()) {
                    PlayerFight fight1 = iterator.next();

                    if (fight1.expired()) {
                        iterator.remove();
                        removeFightCopy(fight1);
                        size--;
                    }
                }
                return size > 0;
            }
        } else {
            return false;
        }
    }

    public boolean isActivelyFighting(SpartanPlayer target, long hit, long damage, boolean both) {
        if (!fights.isEmpty()) {
            boolean hitB = false,
                    damageB = false,
                    hasTarget = target != null;

            for (PlayerFight fight : fights) {
                PlayerOpponent[] opponents = fight.getOpponent(profile);

                if (!hasTarget || opponents[1].player.equals(target)) {
                    if (!damageB && opponents[0].getLastDamage(true) <= damage) {
                        if (both) {
                            damageB = true;

                            if (hitB) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                    if (!hitB && opponents[0].getLastHit(true) <= hit) {
                        if (both) {
                            hitB = true;

                            if (damageB) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isActivelyFighting(SpartanPlayer target, boolean hit, boolean damage, boolean both) {
        return isActivelyFighting(
                target,
                hit ? CombatUtils.combatTimeRequirement : -1L,
                damage ? 2_500L : -1L,
                both
        );
    }

    public PlayerFight getFight(SpartanPlayer opponent) {
        if (!fights.isEmpty()) {
            synchronized (fights) {
                Iterator<PlayerFight> iterator = fights.iterator();

                while (iterator.hasNext()) {
                    PlayerFight fight = iterator.next();

                    if (fight.expired()) {
                        iterator.remove();
                        removeFightCopy(fight);
                    } else if (fight.opponent1.player.equals(opponent)
                            || fight.opponent2.player.equals(opponent)) {
                        return fight;
                    }
                }
            }
        }
        PlayerFight fight = new PlayerFight(
                new PlayerOpponent(profile.getSpartanPlayer()),
                new PlayerOpponent(opponent)
        );
        synchronized (fights) {
            fights.add(fight);
            return fight;
        }
    }

    public void track() {
        if (!fights.isEmpty()) {
            synchronized (fights) {
                Iterator<PlayerFight> iterator = fights.iterator();

                while (iterator.hasNext()) {
                    PlayerFight fight = iterator.next();

                    if (fight.expired()) {
                        iterator.remove();
                        removeFightCopy(fight);
                    }
                }
            }
        }
    }

    void removeFight(PlayerFight fight) {
        synchronized (fights) {
            fights.remove(fight);
        }
    }

    private void removeFightCopy(PlayerFight fight) {
        PlayerCombat combat = fight.getOpponent(profile.getSpartanPlayer())[1].player.getProfile().playerCombat;

        synchronized (combat.fights) {
            combat.fights.remove(fight);
        }
    }
}
