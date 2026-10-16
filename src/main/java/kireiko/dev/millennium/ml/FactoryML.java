package kireiko.dev.millennium.ml;

import ai.idealistic.spartan.Register;
import kireiko.dev.millennium.ml.logic.Logger;
import kireiko.dev.millennium.ml.logic.Millennium;
import kireiko.dev.millennium.ml.logic.ModelML;
import kireiko.dev.millennium.ml.logic.ModelVer;
import kireiko.dev.millennium.ml.logic.RNNModelML;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class FactoryML {
    private static final Map<Integer, Millennium> runnableML = new ConcurrentHashMap<>();

    public static void createModel(int id, int tableSize, ModelVer modelVer) {
        createModel(id, tableSize, 10, modelVer);
    }
    public static Millennium createModel(int id, int tableSize, int stackSize, ModelVer modelVer) {
        Millennium millennium;
        switch (modelVer) {
            case VERSION_5: {
                millennium = new RNNModelML(16, 32);
                break;
            }
            default: {
                millennium = new ModelML(tableSize, stackSize);
                break;
            }
        }
        runnableML.put(id, millennium);
        return millennium;
    }
    public static Millennium getModel(int id) {
        return runnableML.get(id);
    }

    public static void removeModel(int id) {
        runnableML.remove(id);
    }

    @SneakyThrows
    public static Millennium loadFromFile(int id, String fileName, int tableSize, int stackSize, ModelVer modelVer) {
        File dataFolder = Register.plugin.getDataFolder();
        File modelsFolder = new File(dataFolder, "models");
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs();
        }
        File modelFile = new File(modelsFolder, fileName);
        Path modelPath = modelFile.toPath();

        if (modelVer == ModelVer.VERSION_5) {
            RNNModelML model = new RNNModelML(16, 32);
            if (modelFile.exists()) {
                try (InputStream in = Files.newInputStream(modelPath)) {
                    model.load(in);
                    runnableML.put(id, model);
                    Logger.info("ModelML was successfully loaded from " + modelFile.getPath() + " (" + model.parameters() + " parameters)");
                    return model;
                } catch (Exception e) {
                    Logger.error("Failed to load v5 model " + fileName + ": " + e.getMessage() + " | created new model!");
                }
            } else {
                Logger.warn("Model " + fileName + " not found. Creating new model file...");
            }

            model.saveToFile(modelFile.getAbsolutePath());
            runnableML.put(id, model);
            return model;
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(modelPath))) {
            Millennium model = (Millennium) ois.readObject();
            runnableML.put(id, model);
            Logger.info("ModelML was successfully loaded from " + modelFile.getPath() + " (" + model.parameters() + " parameters)");
            return model;
        } catch (Exception e) {
            createModel(id, tableSize, stackSize, modelVer);
            getModel(id).saveToFile(modelFile.getAbsolutePath());
            Logger.error(e.getMessage() + " | created new model!");
            return null;
        }
    }

    @SneakyThrows
    public static Millennium loadFromResources(int id, String fileName, int tableSize, int stackSize, ModelVer modelVer) {
        String resourcePath = "/ml/" + fileName;
        try (InputStream is = Register.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                if (modelVer == ModelVer.VERSION_5) {
                    RNNModelML model = new RNNModelML(16, 32);
                    model.load(is);
                    runnableML.put(id, model);
                    Logger.info("ModelML was successfully loaded from " + resourcePath + " (" + model.parameters() + " parameters)");
                    return model;
                }

                try (ObjectInputStream ois = new ObjectInputStream(is)) {
                    Millennium model = (Millennium) ois.readObject();
                    runnableML.put(id, model);
                    Logger.info("ModelML was successfully loaded from " + resourcePath + " (" + model.parameters() + " parameters)");
                    return model;
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load model from resources: " + e.getMessage() + " | loading from file.");
        }

        return loadFromFile(id, fileName, tableSize, stackSize, modelVer);
    }
}
