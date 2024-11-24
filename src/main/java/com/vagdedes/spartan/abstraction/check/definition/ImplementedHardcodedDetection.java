package com.vagdedes.spartan.abstraction.check.definition;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;

public class ImplementedHardcodedDetection extends HardcodedDetection {

    public ImplementedHardcodedDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedHardcodedDetection(CheckDetection executor, String name, boolean def) {
        super(executor.executor, name, def);
    }

}
