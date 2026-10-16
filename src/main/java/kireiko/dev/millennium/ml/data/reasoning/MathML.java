package kireiko.dev.millennium.ml.data.reasoning;

import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.Pair;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public class MathML {
    public static double getDelta(double a, double b) {
        return Math.abs(Math.abs(a) - Math.abs(b));
    }

    // target delta's: 20:
    public int kolmogorovJiffIdentity(List<? extends Number> collection) {
        List<Float> jiff = Statistics.getJiffDelta(collection, 1);
        return (int) (Statistics.kolmogorovSmirnovTest(jiff, Function.identity()) / 4.0d);
    }

    public int shannonEntropyIdentity(List<? extends Number> collection) {
        return Objects.hashCode((Statistics.getShannonEntropy(collection))) / 10000;
    }

    public int outliersIdentity(List<? extends Number> collection) {
        final Pair<List<Double>, List<Double>> outliers = Statistics.getOutliers(collection);
        final int duplicates = Statistics.getDuplicates(collection);
        return (outliers.getX().size() + outliers.getY().size() + duplicates);
    }

    public int[] smoothIdentity(List<? extends Number> rotations) {
        double oldYawResult = rotations.get(0).doubleValue();
        double yawChangeFirst = Math.abs(rotations.get(0).doubleValue() - rotations.get(1).doubleValue());
        int machineKnownMovement = 0,
                        constantRotations = 0,
                        robotizedAmount = 0, aggressiveAim = 0;
        for (Number rotation : rotations) {
            double yawChange = Math.abs(rotation.doubleValue() - oldYawResult);
            double robotized = Math.abs(yawChange - yawChangeFirst);

            if (robotized < 2 && yawChange > 2.5) robotizedAmount += 1;
            if (robotized < 0.99 && yawChange > 4) machineKnownMovement++;
            if (robotized < 0.02 && yawChange > 3) constantRotations++;
            if (robotized < 2 && yawChange > 3) aggressiveAim++;
            oldYawResult = rotation.doubleValue();
        }
        return new int[]{robotizedAmount, machineKnownMovement, constantRotations, aggressiveAim};
    }

    public int distinctIdentity(List<? extends Number> collection) {
        return Statistics.getDistinct(collection) / 2;
    }
}
