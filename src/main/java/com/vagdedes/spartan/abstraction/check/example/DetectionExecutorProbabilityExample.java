package com.vagdedes.spartan.abstraction.check.example;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class DetectionExecutorProbabilityExample extends ProbabilityDetection {

    DetectionExecutorProbabilityExample(CheckRunner executor) {
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
