package kireiko.dev.millennium.ml.logic.rnn.data.preprocessing;

import kireiko.dev.millennium.ml.logic.rnn.data.SequenceData;

public final class HybridSequencePreprocessor implements SequencePreprocessor {
    private final int inputSize;
    private final StatisticalSequencePreprocessor stat;

    public HybridSequencePreprocessor(int inputSize) {
        this.inputSize = inputSize;
        this.stat = new StatisticalSequencePreprocessor(inputSize);
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

            int winLen = end - start;
            if (winLen < 3) continue;

            double[][] window = new double[winLen][2];
            System.arraycopy(rawVecs, start, window, 0, winLen);

            double[] s = stat.prepare(window).x[0];
            double[] r = rawFeatures(window);
            x[used++] = combine(s, r);
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

    private double[] rawFeatures(double[][] w) {
        double[] f = new double[inputSize];
        int step = Math.max(1, w.length / (inputSize / 2));
        int idx = 0;
        for (int i = 0; i < inputSize / 2 && i * step < w.length; i++) {
            f[idx++] = Math.tanh(w[i * step][0] / 100.0);
            f[idx++] = Math.tanh(w[i * step][1] / 100.0);
        }
        return f;
    }

    private double[] combine(double[] stat, double[] raw) {
        double[] c = new double[inputSize];
        int half = inputSize / 2;
        for (int i = 0; i < half && i < stat.length; i++) c[i] = stat[i];
        for (int i = 0; i < half && i < raw.length; i++) c[half + i] = raw[i] * 0.5;
        return c;
    }
}