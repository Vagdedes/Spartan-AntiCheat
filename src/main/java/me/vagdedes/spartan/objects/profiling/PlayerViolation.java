package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.features.performance.FalsePositiveDetection;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.sql.Timestamp;
import java.util.*;

public class PlayerViolation {

    private final Enums.HackType hackType;
    private final long time;
    private final String information, date, detection;
    private final int level, similarityIdentity, playerIdentity;
    final int divisionIdentity;
    private final boolean lowTPS, isOption;
    private final Collection<Number> numbers;

    public PlayerViolation(String name, Enums.HackType hackType, long time, String information, int level, boolean lowTPS) {
        this.playerIdentity = name.hashCode();
        this.hackType = hackType;
        this.time = time;
        this.date = new Timestamp(time).toString().substring(0, 10);
        this.information = information;
        this.level = level;
        this.lowTPS = lowTPS;

        InformationAnalysis informationAnalysis = new InformationAnalysis(hackType, information);
        this.detection = informationAnalysis.detection;
        this.numbers = informationAnalysis.getNumbers();
        this.isOption = informationAnalysis.isOption(hackType);
        this.similarityIdentity = FalsePositiveDetection.getSimplifiedNumber(hackType, detection, numbers);

        int divisionIdentity = (information.hashCode() * SpartanBukkit.hashCodeMultiplier) + level;
        divisionIdentity = (divisionIdentity * SpartanBukkit.hashCodeMultiplier) + Boolean.hashCode(lowTPS);
        this.divisionIdentity = divisionIdentity;
    }

    public int getPlayerIdentity() {
        return playerIdentity;
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    public long getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public String getInformation() {
        return information;
    }

    public int getLevel() {
        return level;
    }

    public boolean isLowTPS() {
        return lowTPS;
    }

    public boolean isDetectionEnabled() {
        return !isOption || hackType.getCheck().getBooleanOption(detection, null);
    }

    public String getDetection() {
        return detection;
    }

    public List<Number> getNumbersList() {
        return new ArrayList<>(numbers);
    }

    public Set<Number> getNumbersSet() {
        return new TreeSet<>(numbers);
    }

    public int getSimilarityIdentity() {
        return similarityIdentity;
    }
}
