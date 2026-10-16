package kireiko.dev.millennium.ml.logic.rnn.pooling;

import kireiko.dev.millennium.ml.logic.rnn.util.MathOps;

import java.util.Random;

public final class AttentionPooling implements PoolingStrategy {
    private final int outSize;
    public final double[] W;
    public double b;

    public AttentionPooling(int outSize, Random rng) {
        this.outSize = outSize;
        this.W = MathOps.xavierUniform(rng, outSize, outSize, 1);
        this.b = 0.0;
    }

    @Override public int outputSize() { return outSize; }

    @Override
    public double[] forward(double[][] hTime, double[] mask, PoolingCache cache) {
        int T = hTime.length;
        double[] scores = new double[T];
        double max = Double.NEGATIVE_INFINITY;

        for (int t = 0; t < T; t++) {
            if (mask[t] <= 0.5) {
                scores[t] = Double.NEGATIVE_INFINITY;
                continue;
            }
            double s = b;
            for (int i = 0; i < outSize; i++) s += hTime[t][i] * W[i];
            scores[t] = s;
            if (s > max) max = s;
        }

        double sumExp = 0.0;
        double[] a = new double[T];
        for (int t = 0; t < T; t++) {
            if (mask[t] <= 0.5) continue;
            double e = Math.exp(scores[t] - max);
            a[t] = e;
            sumExp += e;
        }
        double inv = 1.0 / (sumExp + 1e-10);
        for (int t = 0; t < T; t++) a[t] *= inv;

        double[] pooled = new double[outSize];
        for (int t = 0; t < T; t++) {
            double at = a[t];
            if (at == 0.0) continue;
            for (int i = 0; i < outSize; i++) pooled[i] += at * hTime[t][i];
        }

        if (cache != null) {
            cache.attnWeights = a;
            cache.attnScores = scores;
        }

        return pooled;
    }

    @Override
    public double[][] backward(double[][] hTime, double[] mask, double[] dPooled, PoolingCache cache, PoolingGrad gAcc) {
        int T = hTime.length;
        double[] a = cache.attnWeights;

        double[][] dH = new double[T][outSize];

        double[] dAlpha = new double[T];
        for (int t = 0; t < T; t++) {
            if (mask[t] <= 0.5) continue;
            double s = 0.0;
            for (int i = 0; i < outSize; i++) s += dPooled[i] * hTime[t][i];
            dAlpha[t] = s;
        }

        double sum = 0.0;
        for (int t = 0; t < T; t++) sum += a[t] * dAlpha[t];

        double[] dScore = new double[T];
        for (int t = 0; t < T; t++) {
            if (mask[t] <= 0.5) continue;
            dScore[t] = a[t] * (dAlpha[t] - sum);
        }

        for (int t = 0; t < T; t++) {
            if (mask[t] <= 0.5) continue;

            double at = a[t];
            for (int i = 0; i < outSize; i++) dH[t][i] += at * dPooled[i];

            double ds = dScore[t];
            for (int i = 0; i < outSize; i++) {
                gAcc.dAttentionW[i] += ds * hTime[t][i];
                dH[t][i] += ds * W[i];
            }
            gAcc.dAttentionB += ds;
        }

        return dH;
    }
}
