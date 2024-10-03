package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public abstract class PatternGeneralization {

    public static final class Comparison {

        public final double
                probability,
                contribution,
                significance,
                outcome;

        Comparison() {
            this.probability = -1.0;
            this.contribution = -1.0;
            this.significance = -1.0;
            this.outcome = -1.0;
        }

        Comparison(PatternGeneralization patternGeneralization,
                   int[] situation,
                   float pattern,
                   double probability) {
            this.probability = probability;
            this.contribution = patternGeneralization.contribution(situation);
            this.significance = patternGeneralization.significance(pattern);
            this.outcome = Math.sqrt(
                    ((this.probability * this.probability)
                    + (this.contribution * this.contribution)
                    + (this.significance * this.significance))
                    / 3.0
            );
        }

        public boolean isValid() {
            return this.outcome != -1.0;
        }

    }

    public final Pattern parent;
    final short scissors;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.profilesSynchronizer = new Object();
        // todo
    }

    final PatternGeneralization clearCopy() {
        try {
            PatternGeneralization patternGeneralization = (PatternGeneralization) this.clone();
            patternGeneralization.clear();
            return patternGeneralization;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    final void clear() {
        // todo
        this.clearProfiles();
    }

    // Separator

    private void increaseImportance(int[] situation, PatternProfile patternProfile, float pattern) {
        // todo
    }

    final void decreaseImportance(int[] situation, int profileHash, float pattern) {
        // todo
    }

    private double significance(float pattern) {
        // todo
        return 0.0;
    }

    final double contribution(int[] situation) {
        // todo
        return 0.0;
    }

    // Separator

    final boolean learn(PlayerProfile profile, int[] situation, double pattern, boolean read) {
        if (this.scissors != 0) {
            pattern = AlgebraUtils.cut(pattern, this.scissors);
        }
        PatternProfile patternProfile = this.customLearn(profile, situation, (float) pattern, read);

        if (patternProfile != null) {
            this.increaseImportance(situation, patternProfile, (float) pattern);
            return true;
        } else {
            return false;
        }
    }

    abstract protected PatternProfile customLearn(PlayerProfile profile, int[] situation,
                                                  float pattern, boolean read);

    // Separator

    public final Comparison comparison(PlayerProfile playerProfile, int[] situation) {
        return this.parent.loaded
                ? this.customComparison(playerProfile, situation)
                : new Comparison();
    }

    protected abstract Comparison customComparison(PlayerProfile playerProfile, int[] situation);

    // Separator

    abstract protected void clearProfiles();

    abstract protected int getProfileCount();

    abstract protected PatternProfile deleteProfile(PlayerProfile profile);

    abstract protected boolean hasPatterns(int[] situation);

    abstract void loadStarting();

    abstract void loadCompleted();

}
