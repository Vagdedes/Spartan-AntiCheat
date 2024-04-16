package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.MapMath;
import com.vagdedes.spartan.abstraction.math.implementation.SetMath;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class PatternGeneralization {

    public static final class Comparison {

        public final double
                probability,
                contribution,
                significance,
                similarity,
                outcome;

        private Comparison() {
            this.probability = 1.0;
            this.contribution = 1.0;
            this.significance = 1.0;
            this.similarity = 1.0;
            this.outcome = 1.0;
        }

        Comparison(PatternGeneralization patternGeneralization,
                   PatternProfile patternProfile,
                   int situation,
                   float pattern,
                   double probability) {
            this.probability = probability;
            this.contribution = patternGeneralization.contribution(situation, patternProfile);
            this.significance = patternGeneralization.significance(situation, pattern);
            this.similarity = patternGeneralization.similarity(situation, patternProfile, pattern);
            this.outcome = ((this.probability * this.contribution) + (this.significance * this.similarity)) / 2.0;
        }

    }

    public final Pattern parent;
    final short scissors;
    private final SetMath situationSignificance, patternSignificance;
    private final Map<Integer, MapMath> contribution;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.situationSignificance = new SetMath(Pattern.globalDataLimitBase, 1.0f);
        this.patternSignificance = new SetMath(Pattern.globalDataLimitBase, 1.0f);
        this.contribution = Collections.synchronizedMap(new HashMap<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.contribution) {
                synchronized (this.situationSignificance) {
                    synchronized (this.patternSignificance) {
                        this.clearProfiles();
                        this.contribution.clear();
                        this.situationSignificance.clear();
                        this.patternSignificance.clear();
                    }
                }
            }
        }
    }

    // Separator

    final boolean learn(PlayerProfile profile,
                        long time,
                        int situation,
                        double pattern,
                        boolean store) {
        if (profile.isLegitimate()) {
            double original = pattern;

            if (this.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.scissors);
            }
            PatternProfile patternProfile = this.customLearn(profile, situation, (float) pattern);

            if (patternProfile != null) {
                this.increaseImportance(situation, patternProfile, (float) pattern);

                if (store) {
                    this.parent.setToFile(profile, time, situation, original);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (this.hasPatterns()) {
                Pattern.deleteFromFile(profile, false);
            }
            return false;
        }
    }

    // Separator

    private void increaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        if (this.situationSignificance.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.situationSignificance) {
                this.situationSignificance.removeLeastSignificant();
                this.situationSignificance.add(situation, patternProfile.hash);
            }
        } else {
            synchronized (this.situationSignificance) {
                this.situationSignificance.add(situation, patternProfile.hash);
            }
        }
        if (this.patternSignificance.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.patternSignificance) {
                this.patternSignificance.removeLeastSignificant();
                this.patternSignificance.add(pattern, patternProfile.hash);
            }
        } else {
            synchronized (this.patternSignificance) {
                this.patternSignificance.add(pattern, patternProfile.hash);
            }
        }
        synchronized (this.contribution) {
            MapMath math = this.contribution.computeIfAbsent(
                    situation,
                    k -> new MapMath(Pattern.individualGlobalDataLimitBase, 1.0f)
            );

            if (math.getParts() == Pattern.individualGlobalDataLimit - 1) {
                math.removeLeastSignificant();
            }
            math.add(patternProfile.hash, Float.hashCode(pattern));
        }
    }

    protected final void decreaseImportance(int situation, int profileHash, float pattern) {
        synchronized (this.situationSignificance) {
            this.situationSignificance.remove(pattern, profileHash);
        }
        synchronized (this.contribution) {
            MapMath math = this.contribution.get(situation);

            if (math != null
                    && math.remove(profileHash, Float.hashCode(pattern))
                    && math.isEmpty()) {
                this.contribution.remove(situation);
            }
        }
    }

    protected final void decreaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        this.decreaseImportance(situation, patternProfile.hash, pattern);
    }

    private double significance(int situation, float pattern) {
        double profileCount = this.getProfileCount();
        int situationCount, patternCount;

        synchronized (this.situationSignificance) {
            situationCount = this.situationSignificance.getCount(situation);
        }
        synchronized (this.patternSignificance) {
            patternCount = this.patternSignificance.getCount(pattern);
        }
        return ((situationCount / profileCount) + (patternCount / profileCount)) / 2.0;
    }

    private double contribution(int situation, PatternProfile patternProfile) {
        synchronized (this.contribution) {
            MapMath math = this.contribution.get(situation);

            if (math != null) {
                return (1.0 - math.getContribution(patternProfile.hash))
                        * (1.0 - math.getSpecificContribution(patternProfile.hash));
            }
        }
        return 0.0;
    }

    private double similarity(int situation, PatternProfile patternProfile, float pattern) {
        synchronized (this.contribution) {
            MapMath math = this.contribution.get(situation);

            if (math != null) {
                return (1.0 - math.getDistance(patternProfile.hash, 0.0))
                        * (1.0 - math.getSpecificDistance(patternProfile.hash, Float.hashCode(pattern), 0.0));
            }
        }
        return 0.0;
    }

    // Separator

    final void deleteProfile(PlayerProfile profile) {
        PatternProfile patternProfile;

        synchronized (this.profilesSynchronizer) {
            patternProfile = this.customDeleteProfile(profile);
        }
        if (patternProfile != null) {
            patternProfile.deleteCustomData();
        }
    }

    abstract protected PatternProfile customDeleteProfile(PlayerProfile profile);

    // Separator

    abstract protected boolean hasPatterns();

    abstract protected boolean hasPatterns(int situation);

    abstract protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern);

    public Comparison comparison(PlayerProfile playerProfile, int situation) {
        return this.parent.isLoaded()
                ? this.customComparison(playerProfile, situation)
                : new Comparison();
    }

    protected abstract Comparison customComparison(PlayerProfile playerProfile, int situation);

    // Separator

    protected final void deletePattern(int situation, int profileHash, float pattern) {
        this.customDeletePattern(situation, pattern);
        this.decreaseImportance(situation, profileHash, pattern);
    }

    abstract protected void customDeletePattern(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

    abstract protected int getProfileCount();

}
