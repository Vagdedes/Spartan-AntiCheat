package kireiko.dev.millennium.ml;

import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.*;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.v5.RNN1Module;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.v5.RNN2Module;
import ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced.ml.modules.v5.RNN3Module;
import kireiko.dev.millennium.ml.data.module.ModuleML;
import kireiko.dev.millennium.ml.logic.Logger;
import kireiko.dev.millennium.ml.logic.Millennium;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ClientML {

    public static final boolean DEV_MODE = false;

    public static final String CLIENT_NAME = "quark-e-4.0-131k-a37k-moe";
    private static final int TABLE_SIZE = 2;

    public static final List<ModuleML> MODEL_LIST = Arrays.asList(
                    new M1Module(),
                    new M2Module(),
                    new M3Module(),
                    new M4Module(),
                    new M5Module(),
                    new MHuge1Module(),
                    new MHuge2Module(),
                    new RNN1Module(),
                    new RNN2Module(),
                    new RNN3Module()
    );

    public void run() {
        long totalWeights = 0;
        for (int i = 0; i < MODEL_LIST.size(); i++) {
            final ModuleML moduleML = MODEL_LIST.get(i);
            Millennium loadedModel;

            if (DEV_MODE) {
                loadedModel = FactoryML.loadFromFile(i, moduleML.getName() + ".dat",
                                TABLE_SIZE, moduleML.getParameterBuffer(), moduleML.getVersion());
            } else {
                loadedModel = FactoryML.loadFromResources(i, moduleML.getName() + ".dat",
                                TABLE_SIZE, moduleML.getParameterBuffer(), moduleML.getVersion());
            }

            if (loadedModel != null) {
                totalWeights += loadedModel.parameters();
            }
        }
        Logger.info(CLIENT_NAME + " loaded!");
        Logger.info("Weights count: " + String.format("%, d", totalWeights));
    }
}
