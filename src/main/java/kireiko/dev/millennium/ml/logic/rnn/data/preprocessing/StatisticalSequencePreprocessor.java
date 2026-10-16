package kireiko.dev.millennium.ml.logic.rnn.data.preprocessing;

import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.ml.data.reasoning.MathML;
import kireiko.dev.millennium.ml.logic.rnn.data.SequenceData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class StatisticalSequencePreprocessor implements SequencePreprocessor {
    private final int inputSize;

    private static final double[] SCALERS = {
                    5.0, 0.5, 10.0, 20.0, 3.0, 10.0, 20.0, 0.5, 1.0, 15.0, 5.0, 2.0, 2.0, 2.0, 2.0, 1.0
    };

    public StatisticalSequencePreprocessor(int inputSize) {
        this.inputSize = inputSize;
    }

    @Override
    public SequenceData prepare(double[][] rawVecs) {
        int totalLen = rawVecs.length;
        int targetWindowSize = 20;
        int numWindows = Math.max(2, totalLen / targetWindowSize);
        numWindows = Math.min(15, numWindows);
        int windowSize = Math.max(15, totalLen / numWindows);

        double[][] x = new double[numWindows][inputSize];
        int used = 0;

        for (int w = 0; w < numWindows; w++) {
            int start = w * windowSize;
            if (start >= totalLen) break;
            int end = (w == numWindows - 1) ? totalLen : Math.min(totalLen, start + windowSize);

            List<Double> magnitudes = new ArrayList<>(end - start);
            for(int i = start; i < end; i++) {
                magnitudes.add(Math.hypot(rawVecs[i][0], rawVecs[i][1]));
            }

            if (magnitudes.size() < 3) continue;
            x[used++] = extract(magnitudes);
        }

        double[] mask;
        double[][] out;
        if (used < 2) {
            int targetLen = Math.max(2, used);
            out = new double[targetLen][inputSize];
            mask = new double[targetLen];
            if (used > 0) {
                System.arraycopy(x, 0, out, 0, used);
            }
            for(int i = 0; i < used; i++) mask[i] = 1.0;
        } else {
            out = new double[used][inputSize];
            System.arraycopy(x, 0, out, 0, used);
            mask = new double[used];
            for (int i = 0; i < used; i++) mask[i] = 1.0;
        }

        return new SequenceData(out, mask);
    }

    private double[] extract(List<Double> seq) {
        double[] f = new double[inputSize];
        int idx = 0;

        f[idx++] = safe(() -> MathML.shannonEntropyIdentity(seq));
        f[idx++] = safe(() -> MathML.kolmogorovJiffIdentity(seq));
        f[idx++] = safe(() -> MathML.outliersIdentity(seq));
        f[idx++] = safe(() -> MathML.distinctIdentity(seq));
        f[idx++] = safe(() -> Statistics.getSkewness(seq));
        f[idx++] = safe(() -> Statistics.getKurtosis(seq));
        f[idx++] = safe(() -> Statistics.getStandardDeviation(seq));
        f[idx++] = safe(() -> Statistics.getGiniIndex(seq));
        f[idx++] = safe(() -> Statistics.getCoefficientOfVariation(seq));
        f[idx++] = safe(() -> Statistics.getIQR(seq));
        f[idx++] = safe(() -> Math.abs(Statistics.getAverage(seq) - Statistics.getModeDouble(seq.toArray(new Double[0]))));

        int[] smooth = MathML.smoothIdentity(seq);
        for (int v : smooth) if (idx < inputSize) f[idx++] = v;

        while (idx < inputSize) f[idx++] = 0.0;

        for (int i = 0; i < inputSize; i++) {
            double s = i < SCALERS.length ? SCALERS[i] : 1.0;
            double n = f[i] / s;
            f[i] = n / (1.0 + Math.abs(n));
        }

        return f;
    }

    private double safe(Supplier<Number> s) {
        try {
            Number n = s.get();
            if (n == null) return 0.0;
            double v = n.doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
            return v;
        } catch (Exception e) {
            return 0.0;
        }
    }
}