package com.vagdedes.spartan.utils.math;

public class Sensitivity {
    public static float getGCD() {
        float f1 = (float) (0.5F * 0.6 + 0.2);
        return f1 * f1 * f1 * 8.0F;
    }
    public static float getGCDValue(int m) {
        return (getGCD() * 0.15F) * (float) m;
    }
    public static int calculateSensitivity(float deltaYaw) {
        for (int i = 1; i <= 200; i++) {
            float gcdValue = getGCDValue(i);
            float sensitivityRaw = deltaYaw / gcdValue;
            int sensitivity = (int) sensitivityRaw;
            if (Math.abs(sensitivityRaw - sensitivity) < 0.001) {
                return i;
            }
        }
        return -1;
    }
}
