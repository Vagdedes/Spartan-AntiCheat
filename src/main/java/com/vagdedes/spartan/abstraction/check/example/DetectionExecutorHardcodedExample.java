package com.vagdedes.spartan.abstraction.check.example;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;

public class DetectionExecutorHardcodedExample extends HardcodedDetection {

    DetectionExecutorHardcodedExample(CheckRunner executor) {
        super(
                executor,
                null,
                null,
                "detection_option_name_in_checks_yml",
                true // Enabled By Default Or Not
        );
    }

}
