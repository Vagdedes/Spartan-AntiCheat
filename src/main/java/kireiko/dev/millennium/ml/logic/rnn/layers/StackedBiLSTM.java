package kireiko.dev.millennium.ml.logic.rnn.layers;

import java.util.Random;

public final class StackedBiLSTM {
    public final int inputSize;
    public final int hiddenSize;
    public final int numLayers;
    public final boolean bidirectional;

    public final LSTMLayer[] fwd;
    public final LSTMLayer[] bwd;

    public static final class Cache {
        public LSTMLayer.Cache[] fwdCache;
        public LSTMLayer.Cache[] bwdCache;
        public double[][][] layerOutputs;
        public double[] dropoutMask;
    }

    public StackedBiLSTM(int inputSize, int hiddenSize, int numLayers, boolean bidirectional, Random rng) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.numLayers = numLayers;
        this.bidirectional = bidirectional;

        fwd = new LSTMLayer[numLayers];
        bwd = bidirectional ? new LSTMLayer[numLayers] : null;

        for (int l = 0; l < numLayers; l++) {
            int in = (l == 0) ? inputSize : (bidirectional ? hiddenSize * 2 : hiddenSize);
            fwd[l] = new LSTMLayer(in, hiddenSize, rng, l * hiddenSize);
            if (bidirectional) bwd[l] = new LSTMLayer(in, hiddenSize, rng, l * hiddenSize);
        }
    }

    public int outputSize() {
        return bidirectional ? hiddenSize * 2 : hiddenSize;
    }

    public double[][] forward(double[][] x, boolean training, double dropoutRate, double recDropRate, Random rng, Cache cache) {
        double[][] cur = x;

        double[] dropMask = null;
        if (training && dropoutRate > 0) {
            int os = outputSize();
            dropMask = new double[os];
            double keep = 1.0 - dropoutRate;
            for (int i = 0; i < os; i++) dropMask[i] = rng.nextDouble() < keep ? (1.0 / keep) : 0.0;
        }

        LSTMLayer.Cache[] fCaches = cache != null ? new LSTMLayer.Cache[numLayers] : null;
        LSTMLayer.Cache[] bCaches = cache != null && bidirectional ? new LSTMLayer.Cache[numLayers] : null;
        double[][][] layerOut = cache != null ? new double[numLayers][][] : null;

        for (int l = 0; l < numLayers; l++) {
            LSTMLayer.Cache fc = cache != null ? (fCaches[l] = new LSTMLayer.Cache()) : null;
            double[][] fo = fwd[l].forward(cur, false, training, recDropRate, rng, fc);

            double[][] out;
            if (bidirectional) {
                LSTMLayer.Cache bc = cache != null ? (bCaches[l] = new LSTMLayer.Cache()) : null;
                double[][] bo = bwd[l].forward(cur, true, training, recDropRate, rng, bc);
                out = new double[fo.length][hiddenSize * 2];
                for (int t = 0; t < fo.length; t++) {
                    System.arraycopy(fo[t], 0, out[t], 0, hiddenSize);
                    System.arraycopy(bo[t], 0, out[t], hiddenSize, hiddenSize);
                }
            } else {
                out = fo;
            }

            if (training && dropMask != null && l < numLayers - 1) {
                for (int t = 0; t < out.length; t++) for (int i = 0; i < out[t].length; i++) out[t][i] *= dropMask[i % dropMask.length];
            }

            if (cache != null) layerOut[l] = out;
            cur = out;
        }

        if (cache != null) {
            cache.fwdCache = fCaches;
            cache.bwdCache = bCaches;
            cache.layerOutputs = layerOut;
            cache.dropoutMask = dropMask;
        }

        return cur;
    }

    public static final class Grad {
        public final LSTMLayer.Grad[] fwd;
        public final LSTMLayer.Grad[] bwd;

        public Grad(StackedBiLSTM m) {
            fwd = new LSTMLayer.Grad[m.numLayers];
            bwd = m.bidirectional ? new LSTMLayer.Grad[m.numLayers] : null;
            for (int l = 0; l < m.numLayers; l++) {
                int in = (l == 0) ? m.inputSize : (m.bidirectional ? m.hiddenSize * 2 : m.hiddenSize);
                fwd[l] = new LSTMLayer.Grad(in, m.hiddenSize);
                if (m.bidirectional) bwd[l] = new LSTMLayer.Grad(in, m.hiddenSize);
            }
        }
    }

    public double[][] backward(Cache cache, double[][] dOut_time, Grad gAcc, double clip) {
        double[][] dCur = dOut_time;

        for (int l = numLayers - 1; l >= 0; l--) {
            if (cache.dropoutMask != null && l < numLayers - 1) {
                for (int t = 0; t < dCur.length; t++) for (int i = 0; i < dCur[t].length; i++) dCur[t][i] *= cache.dropoutMask[i % cache.dropoutMask.length];
            }

            if (bidirectional) {
                double[][] dF = new double[dCur.length][hiddenSize];
                double[][] dB = new double[dCur.length][hiddenSize];
                for (int t = 0; t < dCur.length; t++) {
                    System.arraycopy(dCur[t], 0, dF[t], 0, hiddenSize);
                    System.arraycopy(dCur[t], hiddenSize, dB[t], 0, hiddenSize);
                }

                double[][] dXF = fwd[l].backward(cache.fwdCache[l], dF, gAcc.fwd[l], clip);
                double[][] dXB = bwd[l].backward(cache.bwdCache[l], dB, gAcc.bwd[l], clip);

                int in = (l == 0) ? inputSize : hiddenSize * 2;
                double[][] dNext = new double[dCur.length][in];
                for (int t = 0; t < dCur.length; t++) for (int i = 0; i < in; i++) dNext[t][i] = dXF[t][i] + dXB[t][i];
                dCur = dNext;
            } else {
                dCur = fwd[l].backward(cache.fwdCache[l], dCur, gAcc.fwd[l], clip);
            }
        }

        return dCur;
    }
}