package com.vagdedes.spartan.abstraction.check.example;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class DetectionExecutorExample extends DetectionExecutor {

    DetectionExecutorExample(CheckExecutor executor) {
        super(
                executor,
                "detection_option_name_in_checks_yml",
                true // Enabled By Default Or Not
        );
    }

    void customMethod1() {

    }

    boolean customMethod2() {
        return true;
    }
}
