package kireiko.dev.millennium.ml.data.module;

import kireiko.dev.millennium.ml.data.ResultML;
import kireiko.dev.millennium.ml.logic.ModelVer;

public interface ModuleML {
    String getName();
    ModuleResultML getResult(ResultML resultML);
    int getParameterBuffer();
    default ModelVer getVersion() {
        return ModelVer.VERSION_4_5;
    }
}
