package com.vagdedes.spartan.abstraction.check.definition;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class ImplementedProbabilityDetection extends ProbabilityDetection {

    public ImplementedProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedProbabilityDetection(CheckDetection executor, String name, boolean def) {
        super(executor.executor, name, def);
    }

}
