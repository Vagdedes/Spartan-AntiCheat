package com.vagdedes.spartan.abstraction.math;

public interface AbstractMath {

    void clear();

    Number removeLeastSignificant();

    boolean isEmpty();

    int getParts();

    int getTotal();

    int getCount(Number number);

    // Separator

    double getGeneralContribution();

    double getUniqueContribution();

    double getGeneralContribution(Number ignoreNumber);

    double getPersonalContribution(Number number);

    double getProbability(Number number, double defaultValue);

    double getCumulativeProbability(Number number, double defaultValue);

    double getZScore(Number number, double defaultValue);

}

