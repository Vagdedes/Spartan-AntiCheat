package kireiko.dev.millennium.ml.logic.rnn.pooling;

public final class MeanPooling implements PoolingStrategy {
    private final int outSize;

    public MeanPooling(int outSize) { this.outSize = outSize; }

    @Override public int outputSize() { return outSize; }

    @Override
    public double[] forward(double[][] hTime, double[] mask, PoolingCache cache) {
        double[] pooled = new double[outSize];
        double cnt = 0.0;
        for (int t = 0; t < hTime.length; t++) {
            if (mask[t] > 0.5) {
                for (int i = 0; i < outSize; i++) pooled[i] += hTime[t][i];
                cnt += 1.0;
            }
        }
        if (cnt > 0) for (int i = 0; i < outSize; i++) pooled[i] /= cnt;
        return pooled;
    }

    @Override
    public double[][] backward(double[][] hTime, double[] mask, double[] dPooled, PoolingCache cache, PoolingGrad gAcc) {
        double cnt = 0.0;
        for (int t = 0; t < hTime.length; t++) if (mask[t] > 0.5) cnt += 1.0;
        if (cnt <= 0) cnt = 1.0;

        double[][] dH = new double[hTime.length][outSize];
        double s = 1.0 / cnt;
        for (int t = 0; t < hTime.length; t++) {
            if (mask[t] > 0.5) for (int i = 0; i < outSize; i++) dH[t][i] = dPooled[i] * s;
        }
        return dH;
    }
}
