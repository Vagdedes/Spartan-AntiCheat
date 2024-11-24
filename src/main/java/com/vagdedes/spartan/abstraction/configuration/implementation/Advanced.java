package com.vagdedes.spartan.abstraction.configuration.implementation;

import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;

public class Advanced extends ConfigurationBuilder {

    public Advanced() {
        super("advanced");
    }

    @Override
    public void create() {
        clear();

        //addOption("path.to.option", true);
    }

}
