package com.vagdedes.spartan.abstraction.check.definition;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class ImplementedProbabilityDetection extends ProbabilityDetection {

    public ImplementedProbabilityDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        super(executor, forcedDataType, detectionType, name, def);
    }

    public ImplementedProbabilityDetection(
            CheckDetection executor,
            String name,
            Boolean def
    ) {
        super(executor.executor, executor.forcedDataType, executor.detectionType, name, def);

        if (!executor.hasName) {
            executor.executor.removeDetection(executor);
        }
    }

}
