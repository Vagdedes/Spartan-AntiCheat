package kireiko.dev.millennium.ml.logic.rnn.pooling;

import java.util.Arrays;

public final class MaxPooling implements PoolingStrategy {
    private final int outSize;
    private int[] argmax;

    public MaxPooling(int outSize) { this.outSize = outSize; }

    @Override public int outputSize() { return outSize; }

    @Override
    public double[] forward(double[][] hTime, double[] mask, PoolingCache cache) {
        double[] pooled = new double[outSize];
        Arrays.fill(pooled, Double.NEGATIVE_INFINITY);
        argmax = new int[outSize];
        Arrays.fill(argmax, -1);

        for (int t = 0; t < hTime.length; t++) {
            if (mask[t] <= 0.5) continue;
            for (int i = 0; i < outSize; i++) {
                if (hTime[t][i] > pooled[i]) {
                    pooled[i] = hTime[t][i];
                    argmax[i] = t;
                }
            }
        }

        for (int i = 0; i < outSize; i++) if (Double.isInfinite(pooled[i])) pooled[i] = 0.0;
        return pooled;
    }

    @Override
    public double[][] backward(double[][] hTime, double[] mask, double[] dPooled, PoolingCache cache, PoolingGrad gAcc) {
        double[][] dH = new double[hTime.length][outSize];
        for (int i = 0; i < outSize; i++) {
            int t = argmax[i];
            if (t >= 0) dH[t][i] += dPooled[i];
        }
        return dH;
    }
}
