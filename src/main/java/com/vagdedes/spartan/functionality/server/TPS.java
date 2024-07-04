package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class TPS {

    public static final long tickTime = 50L;
    public static final int tickTimeInteger = (int) tickTime;
    public static final double
            maximum = 20.0,
            tickTimeDecimal = (double) tickTime;
    private static final long initiation = System.currentTimeMillis();

    public static long tick() {
        return AlgebraUtils.integerFloor((System.currentTimeMillis() - initiation) / (double) tickTime);
    }

}
