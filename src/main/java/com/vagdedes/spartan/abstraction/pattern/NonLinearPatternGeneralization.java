package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.NumberMath;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NonLinearPatternGeneralization extends PatternGeneralization { // todo change

    private final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    private final Map<Integer, NumberMath> correlations;

    protected NonLinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new HashMap<>();
        this.correlations = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    protected PatternProfile customDeleteProfile(PlayerProfile profile) {
        return this.profiles.remove(profile);
    }

    @Override
    protected boolean hasPatterns() {
        return !this.correlations.isEmpty();
    }

    @Override
    protected boolean hasPatterns(int situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                NumberMath pie = this.correlations.computeIfAbsent(
                        situation,
                        k -> new NumberMath(Pattern.individualGlobalDataLimitBase, 1.0f)
                );
                if (pie.getParts() == Pattern.individualGlobalDataLimit - 1) {
                    this.decreaseImportance(
                            situation,
                            patternProfile,
                            pie.removeLeastSignificant()
                    );
                }
                pie.add(pattern);
            }
            synchronized (patternProfile.correlations) {
                patternProfile.correlations.put(situation, pattern);
            }
            return patternProfile;
        } else {
            return null;
        }
    }

    @Override
    public Comparison customComparison(PlayerProfile profile, int situation) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);
        float pattern;

        synchronized (patternProfile.correlations) {
            Float cache = patternProfile.correlations.get(situation);

            if (cache != null) {
                pattern = cache;
            } else {
                return null;
            }
        }
        synchronized (this.correlations) {
            NumberMath pie = this.correlations.get(situation);

            if (pie == null) {
                return new Comparison(
                        this,
                        patternProfile,
                        situation,
                        pattern,
                        0.0
                );
            } else {
                return new Comparison(
                        this,
                        patternProfile,
                        situation,
                        pattern,
                        pie.getRatio(pattern, 0.0)
                );
            }
        }
    }

    @Override
    protected void customDeletePattern(int situation, float pattern) {
        synchronized (this.correlations) {
            NumberMath pie = this.correlations.get(situation);

            if (pie != null
                    && pie.remove(pattern)
                    && pie.isEmpty()) {
                this.correlations.remove(situation);
            }
        }
    }

    @Override
    protected void clearProfiles() {
        this.profiles.clear();
    }

    @Override
    protected Collection<? extends PatternProfile> getProfiles() {
        return this.profiles.values();
    }

    @Override
    protected int getProfileCount() {
        return this.profiles.size();
    }

    // Separator

    private NonLinearPatternProfile getLocalProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new NonLinearPatternProfile(this, profile)
            );
        }
    }

}
