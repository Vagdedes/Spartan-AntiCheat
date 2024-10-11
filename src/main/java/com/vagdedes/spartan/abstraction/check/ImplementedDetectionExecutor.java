package com.vagdedes.spartan.abstraction.check;

public class ImplementedDetectionExecutor extends DetectionExecutor {

    public ImplementedDetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedDetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

}
