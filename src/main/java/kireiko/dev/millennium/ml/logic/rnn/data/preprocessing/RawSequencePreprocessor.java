package kireiko.dev.millennium.ml.logic.rnn.data.preprocessing;

import kireiko.dev.millennium.ml.logic.rnn.data.SequenceData;
import kireiko.dev.millennium.ml.logic.rnn.util.MathOps;
import lombok.Getter;

import java.util.Arrays;
import java.util.Random;

public final class RawSequencePreprocessor implements SequencePreprocessor {
    private final int inputSize;
    @Getter
    private final double[] embeddingWy;
    @Getter
    private final double[] embeddingWp;
    @Getter
    private final double[] embeddingB;

    public RawSequencePreprocessor(int inputSize, Random rng) {
        this.inputSize = inputSize;
        this.embeddingWy = MathOps.xavierUniform(rng, inputSize, 1, inputSize);
        this.embeddingWp = MathOps.xavierUniform(rng, inputSize, 1, inputSize);
        this.embeddingB = new double[inputSize];
    }

    @Override
    public SequenceData prepare(double[][] rawVecs) {
        int T = rawVecs.length;
        double[][] x = new double[T][inputSize];
        for (int t = 0; t < T; t++) {
            double vY = Math.tanh(rawVecs[t][0] / 100.0);
            double vP = Math.tanh(rawVecs[t][1] / 100.0);
            for (int i = 0; i < inputSize; i++) {
                x[t][i] = vY * embeddingWy[i] + vP * embeddingWp[i] + embeddingB[i];
            }
        }
        double[] mask = new double[T];
        Arrays.fill(mask, 1.0);
        return new SequenceData(x, mask);
    }
}