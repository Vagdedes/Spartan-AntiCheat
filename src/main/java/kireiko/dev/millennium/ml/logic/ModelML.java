package kireiko.dev.millennium.ml.logic;

import kireiko.dev.millennium.ml.data.DataML;
import kireiko.dev.millennium.ml.data.ObjectML;
import kireiko.dev.millennium.ml.data.ResultML;
import kireiko.dev.millennium.ml.data.statistic.StatisticML;
import kireiko.dev.millennium.types.Pair;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

@Data
public class ModelML implements Serializable, Millennium {
    private static final long serialVersionUID = 1L;
    private List<DataML> table;
    public ModelML(int tableSize, int stackSize) {
        this.table = new LinkedList<>();
        for (int i = 0; i < tableSize; i++)
            this.table.add(new DataML(stackSize));
    }

    @SneakyThrows
    @Override
    public ResultML checkData(final List<ObjectML> o) {
        if (o.size() != this.table.size()) {
            throw new Exception("The number of tables in the input data should be" +
                            " equal to what is specified in the model");
        }
        final ResultML result = new ResultML();
        for (int i = 0; i < this.table.size(); i++) {
            DataML dataML = this.table.get(i);
            ObjectML objectML = o.get(i);
            { // statistics
                final List<Double> p = dataML.checkData(objectML);
                result.statisticsResult.apply(p, this.table.size());
            }
        }
        return result;
    }
    @SneakyThrows
    @Override
    public void learnByData(List<ObjectML> o, boolean isMustBeBlocked) {
        Logger.info("Training a model...");
        if (o.size() != this.table.size()) {
            throw new Exception("The number of tables in the input data should be" +
                            " equal to what is specified in the model");
        }
        for (int i = 0; i < this.table.size(); i++) {
            DataML dataML = this.table.get(i);
            ObjectML objectML = o.get(i);
            dataML.pushData(objectML, isMustBeBlocked);
            Logger.info("Loaded data into table no. " + i);
        }
        Logger.info("Training completed!");
        int trained = 0, fixed = 0;
        for (DataML dataML : this.table) {
            for (StatisticML statisticML : dataML.getStatisticTable()) {
                trained += statisticML.getLearned();
                fixed += statisticML.getFixed();
            }
        }
        Logger.info("Trained " + trained + " patterns.");
        Logger.info("Fixed " + fixed + " false positives.");
    }

    @SneakyThrows
    @Override
    public void saveToFile(String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(fileName)))) {
            oos.writeObject(this);
            Logger.info("ModelML was successfully saved to " + fileName);
        }
    }

    @Override
    public int parameters() {
        int i = 0;
        for (DataML dataML : this.getTable()) {
            for (StatisticML entropyMl : dataML.getStatisticTable()) {
                i += entropyMl.getParameters().size();
            }
        }
        return i;
    }

    @Override
    public void trainEpochs(List<Pair<List<ObjectML>, Boolean>> dataset, int epochs) {
    }
}
