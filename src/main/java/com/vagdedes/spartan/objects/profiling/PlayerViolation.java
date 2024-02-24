package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
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
        this.similarityIdentity = FalsePositiveDetection.getSimplifiedNumber(hackType, detection, numbers);
    }

    boolean isDetectionEnabled() {
        return !isOption || hackType.getCheck().getBooleanOption(detection, null);
    }

    public List<Number> getNumbersList() {
        return new ArrayList<>(numbers);
    }
}
