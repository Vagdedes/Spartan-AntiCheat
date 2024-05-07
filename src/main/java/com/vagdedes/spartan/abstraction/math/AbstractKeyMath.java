package com.vagdedes.spartan.abstraction.math;

public interface AbstractKeyMath extends AbstractMath {

    double getGeneralContribution(Number number, int ignoreHash);

    double getPersonalContribution(Number number, int ignoreHash);
}

