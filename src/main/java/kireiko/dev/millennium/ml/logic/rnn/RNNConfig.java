package kireiko.dev.millennium.ml.logic.rnn;

import kireiko.dev.millennium.ml.logic.RNNModelML;

import java.util.Random;

public final class RNNConfig {
    public int inputSize = 16;
    public int hiddenSize = 64;
    public int numLayers = 2;
    public boolean bidirectional = true;

    public RNNModelML.InputMode inputMode = RNNModelML.InputMode.HYBRID;
    public RNNModelML.PoolingMode poolingMode = RNNModelML.PoolingMode.ATTENTION;

    public double learningRate = 0.0003;
    public double dropoutRate = 0.1;
    public double recurrentDropoutRate = 0.2;
    public double weightDecay = 1e-3;
    public double gradientClip = 5.0;
    public double labelSmoothing = 0.1;

    public long seed = 42L;

    public Random rng() {
        return new Random(seed);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final RNNConfig c = new RNNConfig();
        public Builder inputSize(int v) { c.inputSize = v; return this; }
        public Builder hiddenSize(int v) { c.hiddenSize = v; return this; }
        public Builder numLayers(int v) { c.numLayers = v; return this; }
        public Builder bidirectional(boolean v) { c.bidirectional = v; return this; }
        public Builder inputMode(RNNModelML.InputMode v) { c.inputMode = v; return this; }
        public Builder poolingMode(RNNModelML.PoolingMode v) { c.poolingMode = v; return this; }
        public Builder learningRate(double v) { c.learningRate = v; return this; }
        public Builder dropoutRate(double v) { c.dropoutRate = v; return this; }
        public Builder recurrentDropoutRate(double v) { c.recurrentDropoutRate = v; return this; }
        public Builder weightDecay(double v) { c.weightDecay = v; return this; }
        public Builder gradientClip(double v) { c.gradientClip = v; return this; }
        public Builder labelSmoothing(double v) { c.labelSmoothing = v; return this; }
        public Builder seed(long v) { c.seed = v; return this; }
        public RNNConfig build() { return c; }
    }
}
