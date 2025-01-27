package com.vagdedes.spartan.utils.java;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReflectionUtils {

    public static Class<?> getClass(String s) {
        try {
            return Class.forName(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean classExists(String s) {
        try {
            Class.forName(s);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

}
