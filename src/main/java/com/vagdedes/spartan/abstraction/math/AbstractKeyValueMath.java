package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyValueMath extends AbstractKeyMath {

    int getSpecificTotal();

    int getSpecificCount(Number number);

    int getSpecificCount(Number number, int hash);

    double getSpecificProbability(Number number, double defaultValue);

    double getPersonalProbability(Number number, int hash, double defaultValue);

    double getSpecificGeneralContribution();

    double getSpecificGeneralContribution(Number ignoreNumber);

    double getSpecificGeneralContribution(Number number, int ignoreHash);

    double getSpecificPersonalContribution(Number number);

    double getSpecificPersonalContribution(Number number, int ignoreHash);
}

