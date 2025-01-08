package com.vagdedes.spartan.utils.java;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

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

    @SneakyThrows
    public static Object getObject(String packages, Class<?> clazz, String path) {
        if (clazz != null && path != null) {
            clazz = Class.forName(packages + clazz.getName());
            final Field field = clazz.getDeclaredField(path);

            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(clazz);
        }
        return null;
    }
}
