package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyValueMath extends AbstractKeyMath {

    int getSpecificCount(Number number, int hash);

    double getSpecificContribution();

    double getSpecificContribution(Number ignoreNumber);

    double getSpecificContribution(Number number, int ignoreHash);

    double getSpecificRatio(Number number, int hash, double defaultValue);

    double getSpecificDistance(Number number, int hash, double defaultValue);

    double getSpecificSlopeProbability(Number number, int hash, double defaultValue);

    double getSpecificCurveProbability(Number number, int hash, double defaultValue);
}

