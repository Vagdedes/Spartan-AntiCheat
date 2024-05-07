package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.NumberMath;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NonLinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    private final Map<Integer, NumberMath> correlations;

    protected NonLinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.correlations = Collections.synchronizedMap(
                new HashMap<>(Pattern.individualDataBase, 1.0f)
        );
    }

    @Override
    protected PatternProfile customLearn(PlayerProfile profile, int[] situation,
                                 float pattern, boolean read) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (!read) {
            patternProfile.lastPattern = pattern;
        }
        synchronized (this.correlations) {
            NumberMath pie = this.correlations.computeIfAbsent(
                    situation[0],
                    k -> new NumberMath(Pattern.individualDataBase, 1.0f)
            );
            if (pie.getParts() == Pattern.individualDataLimit - 1) {
                pie.removeLeastSignificant();
            }
            pie.add(pattern);
        }
        return patternProfile;
    }

    @Override
    public Comparison customComparison(PlayerProfile profile, int[] situation) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.lastPattern == Float.MIN_VALUE) {
            return new Comparison();
        } else {
            synchronized (this.correlations) {
                NumberMath math = this.correlations.get(situation[0]);

                if (math == null) {
                    return new Comparison(
                            this,
                            patternProfile,
                            situation,
                            patternProfile.lastPattern,
                            0.0
                    );
                } else {
                    return new Comparison(
                            this,
                            patternProfile,
                            situation,
                            patternProfile.lastPattern,
                            1.0 - Math.abs(math.getCumulativeProbability(patternProfile.lastPattern, 0.0))
                    );
                }
            }
        }
    }

    @Override
    protected void clearProfiles() {
        synchronized (this.profilesSynchronizer) {
            this.profiles.clear();
        }
    }

    @Override
    protected int getProfileCount() {
        return this.profiles.size();
    }

    @Override
    protected PatternProfile deleteProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.remove(profile);
        }
    }

    @Override
    protected boolean hasPatterns(int[] situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation[0]);
        }
    }

    @Override
    void loadStarting() {

    }

    @Override
    void loadCompleted() {

    }

    // Separator

    private NonLinearPatternProfile getLocalProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new NonLinearPatternProfile(profile)
            );
        }
    }

}
