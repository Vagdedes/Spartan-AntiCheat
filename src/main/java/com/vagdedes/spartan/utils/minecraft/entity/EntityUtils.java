package com.vagdedes.spartan.utils.minecraft.entity;

import com.vagdedes.spartan.utils.java.ReflectionUtils;

public class EntityUtils {

    public static final boolean abstractHorseClass = ReflectionUtils.classExists(
            "org.bukkit.entity.AbstractHorse"
    );

}
