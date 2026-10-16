package kireiko.dev.millennium.ml.logic.rnn.layers;

import kireiko.dev.millennium.ml.logic.rnn.util.MathOps;

import java.util.Random;

public final class BinaryHead {
    public final int in;
    public final double[] V;
    public double bias;

    public static final class Cache {
        public double[] pooled;
        public double logit;
        public double y;
    }

    public BinaryHead(int in, Random rng) {
        this.in = in;
        this.V = MathOps.xavierUniform(rng, in, in, 1);
        this.bias = 0.0;
    }

    public double forward(double[] pooled, Cache c) {
        double logit = bias;
        for (int i = 0; i < in; i++) logit += pooled[i] * V[i];
        double y = MathOps.sigmoid(logit);
        if (c != null) {
            c.pooled = pooled;
            c.logit = logit;
            c.y = y;
        }
        return y;
    }

    public static final class Grad {
        public final double[] dV;
        public double dBias;
        public Grad(int in) { dV = new double[in]; }
    }

    public double[] backward(Cache c, double dLogit, Grad g) {
        g.dBias += dLogit;
        double[] dPooled = new double[in];
        for (int i = 0; i < in; i++) {
            g.dV[i] += dLogit * c.pooled[i];
            dPooled[i] = dLogit * V[i];
        }
        return dPooled;
    }
}
