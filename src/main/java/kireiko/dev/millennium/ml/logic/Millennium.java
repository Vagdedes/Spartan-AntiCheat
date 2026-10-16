package kireiko.dev.millennium.ml.logic;

import kireiko.dev.millennium.ml.data.ObjectML;
import kireiko.dev.millennium.ml.data.ResultML;
import kireiko.dev.millennium.types.Pair;

import java.util.List;

public interface Millennium {
    ResultML checkData(List<ObjectML> o);
    void learnByData(List<ObjectML> o, boolean isMustBeBlocked);
    void trainEpochs(List<Pair<List<ObjectML>, Boolean>> dataset, int epochs);
    void saveToFile(String fileName);
    int parameters();
}
