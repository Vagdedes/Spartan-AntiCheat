package com.vagdedes.spartan.abstraction.math;

public interface AbstractMath {

    void clear();

    Float removeLeastSignificant();

    boolean isEmpty();

    int getParts();

    int getTotal();

    int getCount(Number number);

    // Separator

    double getContribution();

    double getContribution(Number ignoreNumber);

    double getRatio(Number number, double defaultValue);

    double getDistance(Number number, double defaultValue);

    double getSlopeProbability(Number number, double defaultValue);

    double getCurveProbability(Number number, double defaultValue);

}

