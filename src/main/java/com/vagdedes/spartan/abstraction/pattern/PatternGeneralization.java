package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;

public abstract class PatternGeneralization {

    public static String path(String key) {
        return Register.plugin.getDataFolder()
                + "/learning"
                + "/" + key;
    }

    public static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    public static final String[] options = {profileOption, patternOption, situationOption};

    // Separator

    public final String key;
    public final int generalization;


    protected PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
    }
}
