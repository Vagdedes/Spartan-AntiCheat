package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerViolation {

    public final Enums.HackType hackType;
    public final long time;
    public final String information, detection;
    public final int level, similarityIdentity;
    public final boolean isOption;
    private final Collection<Number> numbers;

    public PlayerViolation(Enums.HackType hackType, long time, String information, int level) {
        this.hackType = hackType;
        this.time = time;
        this.information = information;
        this.level = level;

        InformationAnalysis informationAnalysis = new InformationAnalysis(hackType, information);
        this.detection = informationAnalysis.detection;
        this.numbers = informationAnalysis.getNumbers();
        this.isOption = informationAnalysis.isOption(hackType);
        int hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

        if (!this.numbers.isEmpty()) {
            for (Number number : this.numbers) {
                if (number instanceof Double) {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + Double.hashCode(AlgebraUtils.cut(number.doubleValue(), GroundUtils.maxHeightLength));
                } else {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier) + number.intValue();
                }
            }
        }
        this.similarityIdentity = hash;
    }

    boolean isDetectionEnabled() {
        return !isOption || hackType.getCheck().getBooleanOption(detection, null);
    }

    public List<Number> getNumbersList() {
        return new ArrayList<>(numbers);
    }
}
