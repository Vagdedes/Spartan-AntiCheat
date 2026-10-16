package kireiko.dev.millennium.ml.logic;

import kireiko.dev.millennium.ml.data.ObjectML;
import kireiko.dev.millennium.ml.data.ResultML;
import kireiko.dev.millennium.ml.logic.rnn.RNNConfig;
import kireiko.dev.millennium.ml.logic.rnn.data.SequenceData;
import kireiko.dev.millennium.ml.logic.rnn.data.preprocessing.RawSequencePreprocessor;
import kireiko.dev.millennium.ml.logic.rnn.data.preprocessing.SequencePreprocessor;
import kireiko.dev.millennium.ml.logic.rnn.data.preprocessing.StatisticalSequencePreprocessor;
import kireiko.dev.millennium.ml.logic.rnn.layers.BinaryHead;
import kireiko.dev.millennium.ml.logic.rnn.layers.LSTMLayer;
import kireiko.dev.millennium.ml.logic.rnn.layers.StackedBiLSTM;
import kireiko.dev.millennium.ml.logic.rnn.optim.AdamW;
import kireiko.dev.millennium.ml.logic.rnn.pooling.*;
import kireiko.dev.millennium.ml.logic.rnn.util.ModelIO;
import kireiko.dev.millennium.types.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RNNModelML implements Millennium {

    public enum InputMode { RAW_SEQUENCE, STATISTICAL_FEATURES, HYBRID }
    public enum PoolingMode { LAST_HIDDEN, MEAN_POOLING, MAX_POOLING, ATTENTION }

    private static final int MAGIC = 0x524E4E36;
    private static final int VERSION = 6;
    private static final double CHECKPOINT_DECISION_THRESHOLD = 0.60;
    private static final double CHECKPOINT_MIN_RECALL = 0.75;

    private final RNNConfig cfg;
    private final Random rng;

    private final RawSequencePreprocessor rawPre;
    private final StatisticalSequencePreprocessor statPre;
    private final SequencePreprocessor hybridPre;
    private SequencePreprocessor activePre;

    private final StackedBiLSTM encoder;

    private final AttentionPooling attnPooling;
    private final PoolingStrategy lastPooling;
    private final PoolingStrategy meanPooling;
    private final PoolingStrategy maxPooling;
    private PoolingStrategy activePooling;

    private final BinaryHead head;
    private final AdamW opt;

    private long step;
    private int batchSize = 16;

    private final double[] dHeadV;
    private final double[] mHeadV;
    private final double[] vHeadV;
    private final AdamW.DoubleRef headBias;
    private final AdamW.DoubleRef mHeadBias;
    private final AdamW.DoubleRef vHeadBias;

    private final double[] dAttnW;
    private final double[] mAttnW;
    private final double[] vAttnW;
    private final AdamW.DoubleRef attnB;
    private final AdamW.DoubleRef mAttnB;
    private final AdamW.DoubleRef vAttnB;

    private final double[] dEmbWy;
    private final double[] mEmbWy;
    private final double[] vEmbWy;

    private final double[] dEmbWp;
    private final double[] mEmbWp;
    private final double[] vEmbWp;

    private final double[] dEmbB;
    private final double[] mEmbB;
    private final double[] vEmbB;

    private final StackedBiLSTM.Grad encGradAcc;

    private final LayerState[] fwdState;
    private final LayerState[] bwdState;

    private double headBiasGrad;
    private double attnBGrad;
    private double decisionThreshold = CHECKPOINT_DECISION_THRESHOLD;

    public RNNModelML(int inputSize, int hiddenSize) {
        this(RNNConfig.builder().inputSize(inputSize).hiddenSize(hiddenSize).build());
    }

    public RNNModelML(RNNConfig cfg) {
        this.cfg = cfg;
        this.rng = new Random(cfg.seed);

        this.rawPre = new RawSequencePreprocessor(cfg.inputSize, rng);
        this.statPre = new StatisticalSequencePreprocessor(cfg.inputSize);
        this.hybridPre = new HybridPre(rawPre, statPre);
        this.activePre = pickPre(cfg.inputMode);

        this.encoder = new StackedBiLSTM(cfg.inputSize, cfg.hiddenSize, cfg.numLayers, cfg.bidirectional, rng);

        int outSize = encoder.outputSize();

        this.attnPooling = new AttentionPooling(outSize, rng);
        this.lastPooling = new LastHiddenPooling(outSize);
        this.meanPooling = new MeanPooling(outSize);
        this.maxPooling = new MaxPooling(outSize);
        this.activePooling = pickPool(cfg.poolingMode);

        this.head = new BinaryHead(outSize, rng);
        this.opt = new AdamW();

        this.step = 0L;

        this.dHeadV = new double[outSize];
        this.mHeadV = new double[outSize];
        this.vHeadV = new double[outSize];
        this.headBias = new AdamW.DoubleRef(head.bias);
        this.mHeadBias = new AdamW.DoubleRef(0.0);
        this.vHeadBias = new AdamW.DoubleRef(0.0);

        this.dAttnW = new double[outSize];
        this.mAttnW = new double[outSize];
        this.vAttnW = new double[outSize];
        this.attnB = new AdamW.DoubleRef(attnPooling.b);
        this.mAttnB = new AdamW.DoubleRef(0.0);
        this.vAttnB = new AdamW.DoubleRef(0.0);

        this.dEmbWy = new double[cfg.inputSize];
        this.mEmbWy = new double[cfg.inputSize];
        this.vEmbWy = new double[cfg.inputSize];

        this.dEmbWp = new double[cfg.inputSize];
        this.mEmbWp = new double[cfg.inputSize];
        this.vEmbWp = new double[cfg.inputSize];

        this.dEmbB = new double[cfg.inputSize];
        this.mEmbB = new double[cfg.inputSize];
        this.vEmbB = new double[cfg.inputSize];

        this.encGradAcc = new StackedBiLSTM.Grad(encoder);

        int L = cfg.numLayers;
        this.fwdState = new LayerState[L];
        this.bwdState = cfg.bidirectional ? new LayerState[L] : null;

        for (int l = 0; l < L; l++) {
            fwdState[l] = new LayerState(encoder.fwd[l]);
            if (cfg.bidirectional) bwdState[l] = new LayerState(encoder.bwd[l]);
        }
    }

    private SequencePreprocessor pickPre(InputMode m) {
        if (m == InputMode.STATISTICAL_FEATURES) return statPre;
        if (m == InputMode.HYBRID) return hybridPre;
        return rawPre;
    }

    private PoolingStrategy pickPool(PoolingMode m) {
        if (m == PoolingMode.MEAN_POOLING) return meanPooling;
        if (m == PoolingMode.MAX_POOLING) return maxPooling;
        if (m == PoolingMode.ATTENTION) return attnPooling;
        return lastPooling;
    }

    public void setLearningRate(double v) { cfg.learningRate = v; }
    public void setDropoutRate(double v) { cfg.dropoutRate = v; }
    public void setRecurrentDropoutRate(double v) { cfg.recurrentDropoutRate = v; }
    public void setWeightDecay(double v) { cfg.weightDecay = v; }
    public void setGradientClip(double v) { cfg.gradientClip = v; }
    public void setLabelSmoothing(double v) { cfg.labelSmoothing = v; }
    public void setBatchSize(int v) { this.batchSize = Math.max(1, v); }
    public void setDecisionThreshold(double v) { this.decisionThreshold = Math.max(0.0, Math.min(1.0, v)); }
    public double getDecisionThreshold() { return decisionThreshold; }

    public void setInputMode(InputMode mode) {
        cfg.inputMode = mode;
        activePre = pickPre(mode);
    }

    public void setPoolingMode(PoolingMode mode) {
        cfg.poolingMode = mode;
        activePooling = pickPool(mode);
    }

    private double[][] prepareVectors(List<ObjectML> o) {
        if (o == null || o.isEmpty()) return new double[0][0];
        List<Double> yaws = o.get(0).getValues();
        List<Double> pitches = o.size() > 1 ? o.get(1).getValues() : yaws;
        int len = Math.min(yaws.size(), pitches.size());
        double[][] res = new double[len][2];
        for (int i = 0; i < len; i++) {
            res[i][0] = yaws.get(i) == null ? 0.0 : yaws.get(i);
            res[i][1] = pitches.get(i) == null ? 0.0 : pitches.get(i);
        }
        return res;
    }

    @Override
    public ResultML checkData(List<ObjectML> o) {
        ResultML r = new ResultML();
        if (o == null || o.isEmpty()) return r;

        double[][] vecs = prepareVectors(o);
        if (vecs.length < 2) return r;

        double prob = forwardProbability(vecs);

        r.statisticsResult.UNUSUAL = prob;
        r.statisticsResult.STRANGE = 0;
        r.statisticsResult.SUSPECTED = 0;
        r.statisticsResult.SUSPICIOUSLY = 0;

        return r;
    }

    @Override
    public void learnByData(List<ObjectML> o, boolean isMustBeBlocked) {
        if (o == null || o.isEmpty()) return;

        double y = isMustBeBlocked ? 1.0 : 0.0;
        if (cfg.labelSmoothing > 0.0) y = y * (1.0 - cfg.labelSmoothing) + 0.5 * cfg.labelSmoothing;

        double[][] vecs = prepareVectors(o);
        if (vecs.length < 2) return;

        List<Sample> samples = new ArrayList<>();
        samples.add(new Sample(vecs, y));

        trainBatch(samples);
    }

    private double[] trainBatch(List<Sample> batch) {
        zeroBatchGrads();

        int used = 0;
        double batchLoss = 0.0;
        double correct = 0.0;

        for (Sample s : batch) {
            ForwardCache fc = forwardCache(s.vecs, true);
            if (fc == null) continue;

            double p = fc.prob;

            boolean predictedCheat = p >= 0.5;
            boolean actualCheat = s.label >= 0.5;
            if (predictedCheat == actualCheat) {
                correct += 1.0;
            }

            p = Math.max(1e-15, Math.min(1.0 - 1e-15, p));
            double loss = - (s.label * Math.log(p) + (1.0 - s.label) * Math.log(1.0 - p));
            batchLoss += loss;

            double dLogit = (p - s.label);

            BinaryHead.Grad hg = new BinaryHead.Grad(head.in);
            double[] dPooled = head.backward(fc.headCache, dLogit, hg);
            add(dHeadV, hg.dV);
            headBiasGrad += hg.dBias;

            PoolingGrad pg = new PoolingGrad();
            pg.dAttentionW = dAttnW;
            pg.dAttentionB = 0.0;

            double[][] dH = activePooling.backward(fc.hTime, fc.seq.mask, dPooled, fc.poolCache, pg);
            attnBGrad += pg.dAttentionB;

            double[][] dX = encoder.backward(fc.encCache, dH, encGradAcc, cfg.gradientClip);

            if (activePre == rawPre || activePre == hybridPre) {
                accumulateEmbeddingGrad(s.vecs, dX);
            }

            used++;
        }

        if (used > 0) {
            opt.incrementStep();
            applyUpdate(used);
            step++;
            return new double[] { batchLoss / used, correct, used };
        }

        return new double[] { 0.0, 0.0, 0.0 };
    }

    private ForwardCache forwardCache(double[][] raw, boolean training) {
        SequenceData seq = activePre.prepare(raw);
        if (seq == null || seq.length() < 2) return null;

        StackedBiLSTM.Cache encCache = new StackedBiLSTM.Cache();
        double[][] hTime = encoder.forward(seq.x, training, cfg.dropoutRate, cfg.recurrentDropoutRate, rng, encCache);

        PoolingCache pc = new PoolingCache();
        double[] pooled = activePooling.forward(hTime, seq.mask, pc);

        BinaryHead.Cache hc = new BinaryHead.Cache();
        double prob = head.forward(pooled, hc);

        ForwardCache fc = new ForwardCache();
        fc.seq = seq;
        fc.hTime = hTime;
        fc.encCache = encCache;
        fc.poolCache = pc;
        fc.headCache = hc;
        fc.prob = prob;
        return fc;
    }

    private double forwardProbability(double[][] raw) {
        SequenceData seq = activePre.prepare(raw);
        if (seq == null || seq.length() < 2) return 0.5;

        double[][] hTime = encoder.forward(seq.x, false, 0.0, 0.0, rng, null);
        double[] pooled = activePooling.forward(hTime, seq.mask, null);
        return head.forward(pooled, null);
    }

    private void accumulateEmbeddingGrad(double[][] raw, double[][] dX) {
        double[] embWy = rawPre.getEmbeddingWy();
        double[] embWp = rawPre.getEmbeddingWp();
        double[] embB = rawPre.getEmbeddingB();

        int T = Math.min(raw.length, dX.length);
        for (int t = 0; t < T; t++) {
            double vY = Math.tanh(raw[t][0] / 100.0);
            double vP = Math.tanh(raw[t][1] / 100.0);
            for (int i = 0; i < cfg.inputSize; i++) {
                double g = dX[t][i];
                if(g > cfg.gradientClip) g = cfg.gradientClip;
                else if(g < -cfg.gradientClip) g = -cfg.gradientClip;

                dEmbWy[i] += g * vY;
                dEmbWp[i] += g * vP;
                dEmbB[i] += g;
            }
        }
    }

    private void applyUpdate(int used) {
        double inv = 1.0 / used;

        scaleInPlace(dHeadV, inv);
        headBias.value = head.bias;
        opt.step(head.V, dHeadV, mHeadV, vHeadV, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.stepScalar(headBias, headBiasGrad * inv, mHeadBias, vHeadBias, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        head.bias = headBias.value;

        scaleInPlace(dAttnW, inv);
        attnB.value = attnPooling.b;
        opt.step(attnPooling.W, dAttnW, mAttnW, vAttnW, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.stepScalar(attnB, attnBGrad * inv, mAttnB, vAttnB, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        attnPooling.b = attnB.value;

        if (activePre == rawPre || activePre == hybridPre) {
            scaleInPlace(dEmbWy, inv);
            scaleInPlace(dEmbWp, inv);
            scaleInPlace(dEmbB, inv);
            opt.step(rawPre.getEmbeddingWy(), dEmbWy, mEmbWy, vEmbWy, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
            opt.step(rawPre.getEmbeddingWp(), dEmbWp, mEmbWp, vEmbWp, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
            opt.step(rawPre.getEmbeddingB(), dEmbB, mEmbB, vEmbB, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        }

        for (int l = 0; l < cfg.numLayers; l++) {
            updateLayer(encoder.fwd[l], encGradAcc.fwd[l], fwdState[l], inv);
            if (cfg.bidirectional) updateLayer(encoder.bwd[l], encGradAcc.bwd[l], bwdState[l], inv);
        }
    }

    private void updateLayer(LSTMLayer layer, LSTMLayer.Grad g, LayerState s, double inv) {
        scaleInPlace(g.dWf, inv); scaleInPlace(g.dWi, inv); scaleInPlace(g.dWc, inv); scaleInPlace(g.dWo, inv);
        scaleInPlace(g.dUf, inv); scaleInPlace(g.dUi, inv); scaleInPlace(g.dUc, inv); scaleInPlace(g.dUo, inv);
        scaleInPlace(g.dbf, inv); scaleInPlace(g.dbi, inv); scaleInPlace(g.dbc, inv); scaleInPlace(g.dbo, inv);
        scaleInPlace(g.dLnGamma, inv); scaleInPlace(g.dLnBeta, inv);

        opt.step(layer.Wf, g.dWf, s.mWf, s.vWf, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Wi, g.dWi, s.mWi, s.vWi, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Wc, g.dWc, s.mWc, s.vWc, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Wo, g.dWo, s.mWo, s.vWo, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);

        opt.step(layer.Uf, g.dUf, s.mUf, s.vUf, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Ui, g.dUi, s.mUi, s.vUi, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Uc, g.dUc, s.mUc, s.vUc, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.Uo, g.dUo, s.mUo, s.vUo, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);

        opt.step(layer.bf, g.dbf, s.mbf, s.vbf, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.bi, g.dbi, s.mbi, s.vbi, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.bc, g.dbc, s.mbc, s.vbc, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.bo, g.dbo, s.mbo, s.vbo, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);

        opt.step(layer.lnGamma, g.dLnGamma, s.mLnG, s.vLnG, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
        opt.step(layer.lnBeta, g.dLnBeta, s.mLnB, s.vLnB, cfg.learningRate, cfg.weightDecay, cfg.gradientClip);
    }

    private void zeroBatchGrads() {
        zero(dHeadV);
        headBiasGrad = 0.0;

        zero(dAttnW);
        attnBGrad = 0.0;

        zero(dEmbWy);
        zero(dEmbWp);
        zero(dEmbB);

        for (int l = 0; l < encGradAcc.fwd.length; l++) {
            encGradAcc.fwd[l].zero();
            if (cfg.bidirectional) encGradAcc.bwd[l].zero();
        }
    }

    private static void zero(double[] a) { for (int i = 0; i < a.length; i++) a[i] = 0.0; }

    private static void add(double[] dst, double[] src) {
        for (int i = 0; i < dst.length; i++) dst[i] += src[i];
    }

    private static void scaleInPlace(double[] a, double s) {
        for (int i = 0; i < a.length; i++) a[i] *= s;
    }

    @Override
    public void saveToFile(String fileName) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(fileName))))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);

            out.writeInt(cfg.inputSize);
            out.writeInt(cfg.hiddenSize);
            out.writeInt(cfg.numLayers);
            out.writeBoolean(cfg.bidirectional);

            out.writeInt(cfg.inputMode.ordinal());
            out.writeInt(cfg.poolingMode.ordinal());

            out.writeDouble(cfg.learningRate);
            out.writeDouble(cfg.dropoutRate);
            out.writeDouble(cfg.recurrentDropoutRate);
            out.writeDouble(cfg.weightDecay);
            out.writeDouble(cfg.gradientClip);
            out.writeDouble(cfg.labelSmoothing);

            out.writeInt(batchSize);
            out.writeLong(step);
            out.writeLong(opt.t);

            ModelIO.writeArr(out, rawPre.getEmbeddingWy());
            ModelIO.writeArr(out, rawPre.getEmbeddingWp());
            ModelIO.writeArr(out, rawPre.getEmbeddingB());

            writeEncoder(out);
            ModelIO.writeArr(out, attnPooling.W);
            out.writeDouble(attnPooling.b);

            ModelIO.writeArr(out, head.V);
            out.writeDouble(head.bias);
            out.writeDouble(decisionThreshold);
        } catch (Exception ignored) {}
    }

    public void load(InputStream in) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(in))) {
            int magic = dis.readInt();
            int ver = dis.readInt();
            if (magic != MAGIC || ver != VERSION) throw new IllegalStateException("bad model version");

            int inSize = dis.readInt();
            int hid = dis.readInt();
            int layers = dis.readInt();
            boolean bi = dis.readBoolean();

            if (inSize != cfg.inputSize || hid != cfg.hiddenSize || layers != cfg.numLayers || bi != cfg.bidirectional) {
                throw new IllegalStateException("arch mismatch");
            }

            int im = dis.readInt();
            int pm = dis.readInt();
            cfg.inputMode = InputMode.values()[clamp(im, InputMode.values().length)];
            cfg.poolingMode = PoolingMode.values()[clamp(pm, PoolingMode.values().length)];
            activePre = pickPre(cfg.inputMode);
            activePooling = pickPool(cfg.poolingMode);

            cfg.learningRate = dis.readDouble();
            cfg.dropoutRate = dis.readDouble();
            cfg.recurrentDropoutRate = dis.readDouble();
            cfg.weightDecay = dis.readDouble();
            cfg.gradientClip = dis.readDouble();
            cfg.labelSmoothing = dis.readDouble();

            batchSize = Math.max(1, dis.readInt());
            step = dis.readLong();
            opt.t = dis.readLong();

            readInto(dis, rawPre.getEmbeddingWy());
            readInto(dis, rawPre.getEmbeddingWp());
            readInto(dis, rawPre.getEmbeddingB());

            readEncoder(dis);

            readInto(dis, attnPooling.W);
            attnPooling.b = dis.readDouble();

            readInto(dis, head.V);
            head.bias = dis.readDouble();
            try {
                decisionThreshold = Math.max(0.0, Math.min(1.0, dis.readDouble()));
            } catch (EOFException ignored) {
                decisionThreshold = CHECKPOINT_DECISION_THRESHOLD;
            }
        } catch (Exception ignored) {}
    }

    private static int clamp(int v, int n) {
        if (n <= 0) return 0;
        if (v < 0) return 0;
        if (v >= n) return n - 1;
        return v;
    }

    private void writeEncoder(DataOutputStream out) throws Exception {
        out.writeInt(cfg.numLayers);
        out.writeBoolean(cfg.bidirectional);

        for (int l = 0; l < cfg.numLayers; l++) {
            writeLayer(out, encoder.fwd[l]);
            if (cfg.bidirectional) writeLayer(out, encoder.bwd[l]);
        }
    }

    private void readEncoder(DataInputStream in) throws Exception {
        int layers = in.readInt();
        boolean bi = in.readBoolean();
        if (layers != cfg.numLayers || bi != cfg.bidirectional) throw new IllegalStateException("encoder mismatch");

        for (int l = 0; l < cfg.numLayers; l++) {
            readLayer(in, encoder.fwd[l]);
            if (cfg.bidirectional) readLayer(in, encoder.bwd[l]);
        }
    }

    private void writeLayer(DataOutputStream out, LSTMLayer l) throws Exception {
        ModelIO.writeArr(out, l.Wf); ModelIO.writeArr(out, l.Wi); ModelIO.writeArr(out, l.Wc); ModelIO.writeArr(out, l.Wo);
        ModelIO.writeArr(out, l.Uf); ModelIO.writeArr(out, l.Ui); ModelIO.writeArr(out, l.Uc); ModelIO.writeArr(out, l.Uo);
        ModelIO.writeArr(out, l.bf); ModelIO.writeArr(out, l.bi); ModelIO.writeArr(out, l.bc); ModelIO.writeArr(out, l.bo);
        ModelIO.writeArr(out, l.lnGamma); ModelIO.writeArr(out, l.lnBeta);
    }

    private void readLayer(DataInputStream in, LSTMLayer l) throws Exception {
        readInto(in, l.Wf); readInto(in, l.Wi); readInto(in, l.Wc); readInto(in, l.Wo);
        readInto(in, l.Uf); readInto(in, l.Ui); readInto(in, l.Uc); readInto(in, l.Uo);
        readInto(in, l.bf); readInto(in, l.bi); readInto(in, l.bc); readInto(in, l.bo);
        readInto(in, l.lnGamma); readInto(in, l.lnBeta);
    }

    private static void readInto(DataInputStream in, double[] dst) throws Exception {
        double[] a = ModelIO.readArr(in);
        if (a == null) return;
        System.arraycopy(a, 0, dst, 0, Math.min(a.length, dst.length));
    }

    @Override
    public int parameters() {
        long p = 0;

        p += rawPre.getEmbeddingWy().length;
        p += rawPre.getEmbeddingWp().length;
        p += rawPre.getEmbeddingB().length;

        for (int l = 0; l < cfg.numLayers; l++) {
            p += countLayer(encoder.fwd[l]);
            if (cfg.bidirectional) p += countLayer(encoder.bwd[l]);
        }

        p += attnPooling.W.length + 1;
        p += head.V.length + 1;

        if (p > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) p;
    }

    private static long countLayer(LSTMLayer l) {
        long p = 0;
        p += l.Wf.length + l.Wi.length + l.Wc.length + l.Wo.length;
        p += l.Uf.length + l.Ui.length + l.Uc.length + l.Uo.length;
        p += l.bf.length + l.bi.length + l.bc.length + l.bo.length;
        p += l.lnGamma.length + l.lnBeta.length;
        return p;
    }

    private static final class Sample {
        final double[][] vecs;
        final double label;
        Sample(double[][] vecs, double label) {
            this.vecs = vecs;
            this.label = label;
        }
    }

    private static final class ForwardCache {
        SequenceData seq;
        double[][] hTime;
        StackedBiLSTM.Cache encCache;
        PoolingCache poolCache;
        BinaryHead.Cache headCache;
        double prob;
    }

    private static final class LayerState {
        final double[] mWf, vWf, mWi, vWi, mWc, vWc, mWo, vWo;
        final double[] mUf, vUf, mUi, vUi, mUc, vUc, mUo, vUo;
        final double[] mbf, vbf, mbi, vbi, mbc, vbc, mbo, vbo;
        final double[] mLnG, vLnG, mLnB, vLnB;

        LayerState(LSTMLayer l) {
            mWf = new double[l.Wf.length]; vWf = new double[l.Wf.length];
            mWi = new double[l.Wi.length]; vWi = new double[l.Wi.length];
            mWc = new double[l.Wc.length]; vWc = new double[l.Wc.length];
            mWo = new double[l.Wo.length]; vWo = new double[l.Wo.length];

            mUf = new double[l.Uf.length]; vUf = new double[l.Uf.length];
            mUi = new double[l.Ui.length]; vUi = new double[l.Ui.length];
            mUc = new double[l.Uc.length]; vUc = new double[l.Uc.length];
            mUo = new double[l.Uo.length]; vUo = new double[l.Uo.length];

            mbf = new double[l.bf.length]; vbf = new double[l.bf.length];
            mbi = new double[l.bi.length]; vbi = new double[l.bi.length];
            mbc = new double[l.bc.length]; vbc = new double[l.bc.length];
            mbo = new double[l.bo.length]; vbo = new double[l.bo.length];

            mLnG = new double[l.lnGamma.length]; vLnG = new double[l.lnGamma.length];
            mLnB = new double[l.lnBeta.length]; vLnB = new double[l.lnBeta.length];
        }
    }

    private static final class HybridPre implements SequencePreprocessor {
        private final SequencePreprocessor raw;
        private final SequencePreprocessor stat;
        HybridPre(SequencePreprocessor raw, SequencePreprocessor stat) {
            this.raw = raw;
            this.stat = stat;
        }
        @Override
        public SequenceData prepare(double[][] rawVecs) {
            if (rawVecs == null) return null;
            if (rawVecs.length >= 40) return stat.prepare(rawVecs);
            return raw.prepare(rawVecs);
        }
    }

    private static final class LastHiddenPooling implements PoolingStrategy {
        private final int outSize;
        LastHiddenPooling(int outSize) { this.outSize = outSize; }
        @Override public int outputSize() { return outSize; }
        @Override
        public double[] forward(double[][] hTime, double[] mask, PoolingCache cache) {
            int last = -1;
            for (int t = hTime.length - 1; t >= 0; t--) {
                if (mask[t] > 0.5) { last = t; break; }
            }
            if (last < 0) last = hTime.length - 1;
            double[] pooled = new double[outSize];
            System.arraycopy(hTime[last], 0, pooled, 0, outSize);
            return pooled;
        }
        @Override
        public double[][] backward(double[][] hTime, double[] mask, double[] dPooled, PoolingCache cache, PoolingGrad gAcc) {
            double[][] dH = new double[hTime.length][outSize];
            int last = -1;
            for (int t = hTime.length - 1; t >= 0; t--) {
                if (mask[t] > 0.5) { last = t; break; }
            }
            if (last < 0) last = hTime.length - 1;
            System.arraycopy(dPooled, 0, dH[last], 0, outSize);
            return dH;
        }
    }

    private static final class LayerWeightsSnapshot {
        final double[] Wf, Wi, Wc, Wo;
        final double[] Uf, Ui, Uc, Uo;
        final double[] bf, bi, bc, bo;
        final double[] lnGamma, lnBeta;

        LayerWeightsSnapshot(LSTMLayer l) {
            Wf = l.Wf.clone(); Wi = l.Wi.clone(); Wc = l.Wc.clone(); Wo = l.Wo.clone();
            Uf = l.Uf.clone(); Ui = l.Ui.clone(); Uc = l.Uc.clone(); Uo = l.Uo.clone();
            bf = l.bf.clone(); bi = l.bi.clone(); bc = l.bc.clone(); bo = l.bo.clone();
            lnGamma = l.lnGamma.clone(); lnBeta = l.lnBeta.clone();
        }
    }

    private static final class LayerStateSnapshot {
        final double[] mWf, vWf, mWi, vWi, mWc, vWc, mWo, vWo;
        final double[] mUf, vUf, mUi, vUi, mUc, vUc, mUo, vUo;
        final double[] mbf, vbf, mbi, vbi, mbc, vbc, mbo, vbo;
        final double[] mLnG, vLnG, mLnB, vLnB;

        LayerStateSnapshot(LayerState s) {
            mWf = s.mWf.clone(); vWf = s.vWf.clone(); mWi = s.mWi.clone(); vWi = s.vWi.clone();
            mWc = s.mWc.clone(); vWc = s.vWc.clone(); mWo = s.mWo.clone(); vWo = s.vWo.clone();
            mUf = s.mUf.clone(); vUf = s.vUf.clone(); mUi = s.mUi.clone(); vUi = s.vUi.clone();
            mUc = s.mUc.clone(); vUc = s.vUc.clone(); mUo = s.mUo.clone(); vUo = s.vUo.clone();
            mbf = s.mbf.clone(); vbf = s.vbf.clone(); mbi = s.mbi.clone(); vbi = s.vbi.clone();
            mbc = s.mbc.clone(); vbc = s.vbc.clone(); mbo = s.mbo.clone(); vbo = s.vbo.clone();
            mLnG = s.mLnG.clone(); vLnG = s.vLnG.clone(); mLnB = s.mLnB.clone(); vLnB = s.vLnB.clone();
        }
    }

    private static final class TrainingSnapshot {
        long step;
        long optT;
        double decisionThreshold;

        double[] embWy, embWp, embB;
        double[] attnW;
        double attnB;
        double[] headV;
        double headBias;

        double[] mHeadV, vHeadV;
        double mHeadBias, vHeadBias;
        double[] mAttnW, vAttnW;
        double mAttnB, vAttnB;
        double[] mEmbWy, vEmbWy, mEmbWp, vEmbWp, mEmbB, vEmbB;

        LayerWeightsSnapshot[] fwdWeights;
        LayerWeightsSnapshot[] bwdWeights;
        LayerStateSnapshot[] fwdStates;
        LayerStateSnapshot[] bwdStates;
    }

    private static void copyArray(double[] src, double[] dst) {
        if (src == null || dst == null) return;
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
    }

    private static void restoreLayerWeights(LayerWeightsSnapshot s, LSTMLayer l) {
        if (s == null || l == null) return;
        copyArray(s.Wf, l.Wf); copyArray(s.Wi, l.Wi); copyArray(s.Wc, l.Wc); copyArray(s.Wo, l.Wo);
        copyArray(s.Uf, l.Uf); copyArray(s.Ui, l.Ui); copyArray(s.Uc, l.Uc); copyArray(s.Uo, l.Uo);
        copyArray(s.bf, l.bf); copyArray(s.bi, l.bi); copyArray(s.bc, l.bc); copyArray(s.bo, l.bo);
        copyArray(s.lnGamma, l.lnGamma); copyArray(s.lnBeta, l.lnBeta);
    }

    private static void restoreLayerState(LayerStateSnapshot s, LayerState d) {
        if (s == null || d == null) return;
        copyArray(s.mWf, d.mWf); copyArray(s.vWf, d.vWf); copyArray(s.mWi, d.mWi); copyArray(s.vWi, d.vWi);
        copyArray(s.mWc, d.mWc); copyArray(s.vWc, d.vWc); copyArray(s.mWo, d.mWo); copyArray(s.vWo, d.vWo);
        copyArray(s.mUf, d.mUf); copyArray(s.vUf, d.vUf); copyArray(s.mUi, d.mUi); copyArray(s.vUi, d.vUi);
        copyArray(s.mUc, d.mUc); copyArray(s.vUc, d.vUc); copyArray(s.mUo, d.mUo); copyArray(s.vUo, d.vUo);
        copyArray(s.mbf, d.mbf); copyArray(s.vbf, d.vbf); copyArray(s.mbi, d.mbi); copyArray(s.vbi, d.vbi);
        copyArray(s.mbc, d.mbc); copyArray(s.vbc, d.vbc); copyArray(s.mbo, d.mbo); copyArray(s.vbo, d.vbo);
        copyArray(s.mLnG, d.mLnG); copyArray(s.vLnG, d.vLnG); copyArray(s.mLnB, d.mLnB); copyArray(s.vLnB, d.vLnB);
    }

    private TrainingSnapshot captureTrainingSnapshot() {
        TrainingSnapshot s = new TrainingSnapshot();
        s.step = step;
        s.optT = opt.t;
        s.decisionThreshold = decisionThreshold;

        s.embWy = rawPre.getEmbeddingWy().clone();
        s.embWp = rawPre.getEmbeddingWp().clone();
        s.embB = rawPre.getEmbeddingB().clone();

        s.attnW = attnPooling.W.clone();
        s.attnB = attnPooling.b;
        s.headV = head.V.clone();
        s.headBias = head.bias;

        s.mHeadV = mHeadV.clone();
        s.vHeadV = vHeadV.clone();
        s.mHeadBias = mHeadBias.value;
        s.vHeadBias = vHeadBias.value;
        s.mAttnW = mAttnW.clone();
        s.vAttnW = vAttnW.clone();
        s.mAttnB = mAttnB.value;
        s.vAttnB = vAttnB.value;
        s.mEmbWy = mEmbWy.clone();
        s.vEmbWy = vEmbWy.clone();
        s.mEmbWp = mEmbWp.clone();
        s.vEmbWp = vEmbWp.clone();
        s.mEmbB = mEmbB.clone();
        s.vEmbB = vEmbB.clone();

        int L = cfg.numLayers;
        s.fwdWeights = new LayerWeightsSnapshot[L];
        s.fwdStates = new LayerStateSnapshot[L];
        s.bwdWeights = cfg.bidirectional ? new LayerWeightsSnapshot[L] : null;
        s.bwdStates = cfg.bidirectional ? new LayerStateSnapshot[L] : null;

        for (int l = 0; l < L; l++) {
            s.fwdWeights[l] = new LayerWeightsSnapshot(encoder.fwd[l]);
            s.fwdStates[l] = new LayerStateSnapshot(fwdState[l]);
            if (cfg.bidirectional) {
                s.bwdWeights[l] = new LayerWeightsSnapshot(encoder.bwd[l]);
                s.bwdStates[l] = new LayerStateSnapshot(bwdState[l]);
            }
        }

        return s;
    }

    private void restoreTrainingSnapshot(TrainingSnapshot s) {
        if (s == null) return;

        step = s.step;
        opt.t = s.optT;
        decisionThreshold = s.decisionThreshold;

        copyArray(s.embWy, rawPre.getEmbeddingWy());
        copyArray(s.embWp, rawPre.getEmbeddingWp());
        copyArray(s.embB, rawPre.getEmbeddingB());

        copyArray(s.attnW, attnPooling.W);
        attnPooling.b = s.attnB;
        attnB.value = s.attnB;

        copyArray(s.headV, head.V);
        head.bias = s.headBias;
        headBias.value = s.headBias;

        copyArray(s.mHeadV, mHeadV);
        copyArray(s.vHeadV, vHeadV);
        mHeadBias.value = s.mHeadBias;
        vHeadBias.value = s.vHeadBias;
        copyArray(s.mAttnW, mAttnW);
        copyArray(s.vAttnW, vAttnW);
        mAttnB.value = s.mAttnB;
        vAttnB.value = s.vAttnB;
        copyArray(s.mEmbWy, mEmbWy);
        copyArray(s.vEmbWy, vEmbWy);
        copyArray(s.mEmbWp, mEmbWp);
        copyArray(s.vEmbWp, vEmbWp);
        copyArray(s.mEmbB, mEmbB);
        copyArray(s.vEmbB, vEmbB);

        for (int l = 0; l < cfg.numLayers; l++) {
            restoreLayerWeights(s.fwdWeights[l], encoder.fwd[l]);
            restoreLayerState(s.fwdStates[l], fwdState[l]);
            if (cfg.bidirectional) {
                restoreLayerWeights(s.bwdWeights[l], encoder.bwd[l]);
                restoreLayerState(s.bwdStates[l], bwdState[l]);
            }
        }

        zeroBatchGrads();
    }

    private static final class PredictionPair implements Comparable<PredictionPair> {
        final double prob;
        final double label;
        PredictionPair(double prob, double label) {
            this.prob = prob;
            this.label = label;
        }
        @Override
        public int compareTo(PredictionPair o) {
            return Double.compare(o.prob, this.prob);
        }
    }

    private static final class DatasetMetrics {
        double lossSum;
        double avgLoss;
        double acc;
        long tp;
        long tn;
        long fp;
        long fn;
        int used;
        int skippedInvalidOrNonFinite;
        final List<PredictionPair> pairs = new ArrayList<>();
    }

    private static final class ThresholdMetrics {
        double threshold;
        double acc;
        double precision;
        double recall;
        double f1;
        double fpr;
        long tp;
        long tn;
        long fp;
        long fn;
    }

    private ThresholdMetrics selectBestThreshold(List<PredictionPair> pairs, double fallbackThreshold) {
        ThresholdMetrics best = evaluateThreshold(pairs, fallbackThreshold);
        if (pairs == null || pairs.isEmpty()) return best;

        List<PredictionPair> sorted = new ArrayList<>(pairs);
        sorted.sort((a, b) -> Double.compare(b.prob, a.prob));

        long totalPos = 0;
        long totalNeg = 0;
        for (PredictionPair pair : sorted) {
            if (pair.label >= 0.5) totalPos++;
            else totalNeg++;
        }

        long tp = 0;
        long fp = 0;
        int i = 0;
        while (i < sorted.size()) {
            double score = sorted.get(i).prob;
            while (i < sorted.size() && Double.compare(sorted.get(i).prob, score) == 0) {
                if (sorted.get(i).label >= 0.5) tp++;
                else fp++;
                i++;
            }

            long fn = totalPos - tp;
            long tn = totalNeg - fp;

            ThresholdMetrics candidate = new ThresholdMetrics();
            candidate.threshold = score;
            candidate.tp = tp;
            candidate.fp = fp;
            candidate.tn = tn;
            candidate.fn = fn;
            fillThresholdMetrics(candidate, totalPos, totalNeg);

            if (isBetterThresholdCandidate(candidate, best)) {
                best = candidate;
            }
        }

        return best;
    }

    private ThresholdMetrics evaluateThreshold(List<PredictionPair> pairs, double threshold) {
        ThresholdMetrics metrics = new ThresholdMetrics();
        metrics.threshold = Math.max(0.0, Math.min(1.0, threshold));
        if (pairs == null || pairs.isEmpty()) return metrics;

        long totalPos = 0;
        long totalNeg = 0;
        for (PredictionPair pair : pairs) {
            boolean actual = pair.label >= 0.5;
            boolean pred = pair.prob >= metrics.threshold;

            if (actual) totalPos++;
            else totalNeg++;

            if (actual && pred) metrics.tp++;
            else if (!actual && !pred) metrics.tn++;
            else if (!actual) metrics.fp++;
            else metrics.fn++;
        }

        fillThresholdMetrics(metrics, totalPos, totalNeg);
        return metrics;
    }

    private void fillThresholdMetrics(ThresholdMetrics metrics, long totalPos, long totalNeg) {
        long total = totalPos + totalNeg;
        metrics.acc = total == 0 ? 0.0 : ((double) (metrics.tp + metrics.tn) / total) * 100.0;
        metrics.precision = (metrics.tp + metrics.fp) == 0 ? 0.0 : (double) metrics.tp / (metrics.tp + metrics.fp);
        metrics.recall = totalPos == 0 ? 0.0 : (double) metrics.tp / totalPos;
        metrics.f1 = (metrics.precision + metrics.recall) == 0.0
                ? 0.0
                : 2.0 * metrics.precision * metrics.recall / (metrics.precision + metrics.recall);
        metrics.fpr = totalNeg == 0 ? 0.0 : (double) metrics.fp / totalNeg;
    }

    private boolean isBetterThresholdCandidate(ThresholdMetrics candidate, ThresholdMetrics best) {
        if (best == null) return true;

        boolean candidateMeetsRecall = candidate.recall >= CHECKPOINT_MIN_RECALL;
        boolean bestMeetsRecall = best.recall >= CHECKPOINT_MIN_RECALL;

        if (candidateMeetsRecall != bestMeetsRecall) return candidateMeetsRecall;
        if (!candidateMeetsRecall) {
            if (candidate.recall > best.recall + 1e-12) return true;
            if (candidate.recall < best.recall - 1e-12) return false;
        }
        if (candidate.fpr < best.fpr - 1e-12) return true;
        if (candidate.fpr > best.fpr + 1e-12) return false;
        if (candidate.precision > best.precision + 1e-12) return true;
        if (candidate.precision < best.precision - 1e-12) return false;
        if (candidate.f1 > best.f1 + 1e-12) return true;
        if (candidate.f1 < best.f1 - 1e-12) return false;
        if (candidate.recall > best.recall + 1e-12) return true;
        if (candidate.recall < best.recall - 1e-12) return false;
        return candidate.threshold > best.threshold + 1e-12;
    }

    private boolean isBetterCheckpoint(ThresholdMetrics candidate, ThresholdMetrics best, double candidatePrAuc, double bestPrAuc,
                                       double candidateRocAuc, double bestRocAuc, double candidateValidLoss, double bestValidLoss) {
        if (best == null) return true;

        boolean candidateMeetsRecall = candidate.recall >= CHECKPOINT_MIN_RECALL;
        boolean bestMeetsRecall = best.recall >= CHECKPOINT_MIN_RECALL;

        if (candidateMeetsRecall != bestMeetsRecall) return candidateMeetsRecall;
        if (!candidateMeetsRecall) {
            if (candidate.recall > best.recall + 1e-12) return true;
            if (candidate.recall < best.recall - 1e-12) return false;
        }
        if (candidate.fpr < best.fpr - 1e-12) return true;
        if (candidate.fpr > best.fpr + 1e-12) return false;
        if (candidate.precision > best.precision + 1e-12) return true;
        if (candidate.precision < best.precision - 1e-12) return false;
        if (candidate.f1 > best.f1 + 1e-12) return true;
        if (candidate.f1 < best.f1 - 1e-12) return false;
        if (candidatePrAuc > bestPrAuc + 1e-12) return true;
        if (candidatePrAuc < bestPrAuc - 1e-12) return false;
        if (candidateRocAuc > bestRocAuc + 1e-12) return true;
        if (candidateRocAuc < bestRocAuc - 1e-12) return false;
        if (candidateValidLoss < bestValidLoss - 1e-12) return true;
        if (candidateValidLoss > bestValidLoss + 1e-12) return false;
        return candidate.threshold > best.threshold + 1e-12;
    }

    private double rocAuc(List<PredictionPair> pairs) {
        if (pairs == null || pairs.isEmpty()) return 0.0;
        List<PredictionPair> sorted = new ArrayList<>(pairs);
        sorted.sort((a, b) -> Double.compare(a.prob, b.prob));

        long pos = 0;
        long neg = 0;
        double sumPosRanks = 0.0;
        int i = 0;
        while (i < sorted.size()) {
            int j = i;
            double score = sorted.get(i).prob;
            while (j + 1 < sorted.size() && Double.compare(sorted.get(j + 1).prob, score) == 0) j++;

            double avgRank = ((double) (i + 1) + (double) (j + 1)) * 0.5;
            for (int k = i; k <= j; k++) {
                PredictionPair p = sorted.get(k);
                if (p.label >= 0.5) {
                    pos++;
                    sumPosRanks += avgRank;
                } else {
                    neg++;
                }
            }
            i = j + 1;
        }

        if (pos == 0 || neg == 0) return 0.0;
        double auc = (sumPosRanks - ((double) pos * (pos + 1) * 0.5)) / ((double) pos * neg);
        if (!Double.isFinite(auc)) return 0.0;
        if (auc < 0.0) return 0.0;
        if (auc > 1.0) return 1.0;
        return auc;
    }

    private double prAuc(List<PredictionPair> pairs) {
        if (pairs == null || pairs.isEmpty()) return 0.0;
        List<PredictionPair> sorted = new ArrayList<>(pairs);
        sorted.sort((a, b) -> Double.compare(b.prob, a.prob));

        long pos = 0;
        for (PredictionPair p : sorted) {
            if (p.label >= 0.5) pos++;
        }
        if (pos == 0) return 0.0;

        long tp = 0;
        long fp = 0;
        double auc = 0.0;
        double prevRecall = 0.0;
        double prevPrecision = 1.0;

        int i = 0;
        while (i < sorted.size()) {
            double score = sorted.get(i).prob;
            long grpPos = 0;
            long grpNeg = 0;
            while (i < sorted.size() && Double.compare(sorted.get(i).prob, score) == 0) {
                if (sorted.get(i).label >= 0.5) grpPos++;
                else grpNeg++;
                i++;
            }

            tp += grpPos;
            fp += grpNeg;
            double recall = (double) tp / pos;
            double precision = (tp + fp) == 0 ? 1.0 : (double) tp / (tp + fp);
            auc += (recall - prevRecall) * (precision + prevPrecision) * 0.5;
            prevRecall = recall;
            prevPrecision = precision;
        }

        if (!Double.isFinite(auc)) return 0.0;
        if (auc < 0.0) return 0.0;
        if (auc > 1.0) return 1.0;
        return auc;
    }

    private DatasetMetrics evaluateDataset(List<Pair<List<ObjectML>, Boolean>> dataset, double decisionThreshold) {
        DatasetMetrics metrics = new DatasetMetrics();
        if (dataset == null || dataset.isEmpty()) return metrics;
        double th = Math.max(0.0, Math.min(1.0, decisionThreshold));

        for (Pair<List<ObjectML>, Boolean> dataPair : dataset) {
            double[][] vecs = prepareVectors(dataPair.getX());
            if (vecs.length < 2) {
                metrics.skippedInvalidOrNonFinite++;
                continue;
            }

            double y = dataPair.getY() ? 1.0 : 0.0;
            double p = forwardProbability(vecs);
            if (!Double.isFinite(p)) {
                metrics.skippedInvalidOrNonFinite++;
                continue;
            }

            p = Math.max(1e-15, Math.min(1.0 - 1e-15, p));
            metrics.lossSum += - (y * Math.log(p) + (1.0 - y) * Math.log(1.0 - p));
            metrics.pairs.add(new PredictionPair(p, y));

            boolean actual = y >= 0.5;
            boolean pred = p >= th;
            if (actual && pred) metrics.tp++;
            else if (!actual && !pred) metrics.tn++;
            else if (!actual) metrics.fp++;
            else metrics.fn++;

            metrics.used++;
        }

        if (metrics.used > 0) {
            metrics.avgLoss = metrics.lossSum / metrics.used;
            metrics.acc = ((double) (metrics.tp + metrics.tn) / metrics.used) * 100.0;
        }
        return metrics;
    }

    private void trainOneEpoch(List<Pair<List<ObjectML>, Boolean>> dataset, int bs, double labelSmoothing) {
        if (dataset == null || dataset.isEmpty()) return;

        java.util.Collections.shuffle(dataset, rng);
        List<Sample> currentBatch = new ArrayList<>(bs);

        for (Pair<List<ObjectML>, Boolean> dataPair : dataset) {
            double y = dataPair.getY() ? 1.0 : 0.0;
            if (labelSmoothing > 0.0) {
                y = y * (1.0 - labelSmoothing) + 0.5 * labelSmoothing;
            }

            double[][] vecs = prepareVectors(dataPair.getX());
            if (vecs.length < 2) continue;

            int chunkSize = 150;
            for (int i = 0; i < vecs.length; i += chunkSize) {
                int end = Math.min(vecs.length, i + chunkSize);
                if (end - i < 2) continue;
                double[][] chunk = new double[end - i][2];
                System.arraycopy(vecs, i, chunk, 0, end - i);

                currentBatch.add(new Sample(chunk, y));

                if (currentBatch.size() >= bs) {
                    trainBatch(currentBatch);
                    currentBatch.clear();
                }
            }
        }
        if (!currentBatch.isEmpty()) {
            trainBatch(currentBatch);
            currentBatch.clear();
        }
    }

    @Override
    public void trainEpochs(List<Pair<List<ObjectML>, Boolean>> dataset, int epochs) {
        if (dataset == null || dataset.isEmpty() || epochs <= 0) return;
        int bs = Math.max(1, batchSize);
        double labelSmoothing = cfg.labelSmoothing;
        double initialDecisionThreshold = this.decisionThreshold;
        TrainingSnapshot initialSnapshot = captureTrainingSnapshot();
        TrainingSnapshot bestSnapshot = null;
        int bestEpoch = -1;
        double bestPrAuc = Double.NEGATIVE_INFINITY;
        double bestRocAuc = 0.0;
        ThresholdMetrics bestThresholdMetrics = null;
        double bestValidLoss = Double.POSITIVE_INFINITY;

        List<Pair<List<ObjectML>, Boolean>> shuffled = new ArrayList<>(dataset);
        java.util.Collections.shuffle(shuffled, rng);

        int total = shuffled.size();
        int splitIndex;
        if (total == 1) {
            splitIndex = 1;
        } else {
            splitIndex = (int) Math.floor(total * 0.8);
            splitIndex = Math.max(1, Math.min(splitIndex, total - 1));
        }

        List<Pair<List<ObjectML>, Boolean>> trainSet = new ArrayList<>(shuffled.subList(0, splitIndex));
        List<Pair<List<ObjectML>, Boolean>> validSet = splitIndex < total
                ? new ArrayList<>(shuffled.subList(splitIndex, total))
                : java.util.Collections.emptyList();

        Logger.info("Dataset split: " + trainSet.size() + " training samples, " + validSet.size() + " validation samples.");

        for (int e = 0; e < epochs; e++) {
            trainOneEpoch(trainSet, bs, labelSmoothing);

            DatasetMetrics trainMetrics = evaluateDataset(trainSet, initialDecisionThreshold);
            DatasetMetrics validMetrics = evaluateDataset(validSet, initialDecisionThreshold);
            ThresholdMetrics thresholdMetrics = selectBestThreshold(validMetrics.pairs, initialDecisionThreshold);

            double avgTrainLoss = trainMetrics.avgLoss;
            double avgTrainAcc = trainMetrics.acc;
            double avgValidLoss = validMetrics.avgLoss;
            double rocAuc = rocAuc(validMetrics.pairs);
            double prAuc = prAuc(validMetrics.pairs);

            Logger.info(String.format("Epoch %d/%d | Train [Loss: %.4f, Acc: %.1f%%] | Valid [Loss: %.4f, Acc: %.1f%%]",
                    (e + 1), epochs, avgTrainLoss, avgTrainAcc, avgValidLoss, thresholdMetrics.acc));
            if (trainMetrics.skippedInvalidOrNonFinite > 0 || validMetrics.skippedInvalidOrNonFinite > 0) {
                Logger.warn(String.format(
                        "Skipped samples -> train invalid/non-finite: %d, validation invalid/non-finite: %d",
                        trainMetrics.skippedInvalidOrNonFinite, validMetrics.skippedInvalidOrNonFinite));
            }
            Logger.info(String.format("Validation Metrics -> Precision: %.4f | Recall: %.4f | F1: %.4f | FPR: %.4f",
                    thresholdMetrics.precision, thresholdMetrics.recall, thresholdMetrics.f1, thresholdMetrics.fpr));
            Logger.info(String.format("Advanced Metrics -> ROC-AUC: %.4f | PR-AUC: %.4f", rocAuc, prAuc));
            Logger.info(String.format("Confusion Matrix -> TP: %d | TN: %d | FP: %d | FN: %d",
                    thresholdMetrics.tp, thresholdMetrics.tn, thresholdMetrics.fp, thresholdMetrics.fn));
            Logger.info(String.format("Decision Threshold -> %.2f (auto-selected, initial: %.2f)",
                    thresholdMetrics.threshold, initialDecisionThreshold));

            if (isBetterCheckpoint(thresholdMetrics, bestThresholdMetrics, prAuc, bestPrAuc, rocAuc, bestRocAuc, avgValidLoss, bestValidLoss)) {
                this.decisionThreshold = thresholdMetrics.threshold;
                bestSnapshot = captureTrainingSnapshot();
                bestEpoch = e + 1;
                bestPrAuc = prAuc;
                bestRocAuc = rocAuc;
                bestThresholdMetrics = thresholdMetrics;
                bestValidLoss = avgValidLoss;
                Logger.info(String.format(
                        "Best checkpoint -> epoch %d selected (thr: %.2f | Precision: %.4f | Recall: %.4f | F1: %.4f | FPR: %.4f | PR-AUC: %.4f | Valid Loss: %.4f)",
                        bestEpoch, thresholdMetrics.threshold, thresholdMetrics.precision, thresholdMetrics.recall,
                        thresholdMetrics.f1, thresholdMetrics.fpr, bestPrAuc, bestValidLoss));
            }
        }

        if (bestSnapshot != null) {
            restoreTrainingSnapshot(bestSnapshot);
            Logger.info(String.format(
                    "Best epoch restored -> %d/%d (thr: %.2f | Precision: %.4f | Recall: %.4f | F1: %.4f | FPR: %.4f | PR-AUC: %.4f | ROC-AUC: %.4f | Valid Acc: %.1f%% | Valid Loss: %.4f)",
                    bestEpoch, epochs, bestThresholdMetrics.threshold, bestThresholdMetrics.precision, bestThresholdMetrics.recall,
                    bestThresholdMetrics.f1, bestThresholdMetrics.fpr, bestPrAuc, bestRocAuc, bestThresholdMetrics.acc, bestValidLoss));

            restoreTrainingSnapshot(initialSnapshot);
            this.decisionThreshold = bestThresholdMetrics.threshold;
            Logger.info(String.format(
                    "Final-fit -> retraining on full dataset for %d epoch(s) with threshold %.2f",
                    bestEpoch, this.decisionThreshold));

            List<Pair<List<ObjectML>, Boolean>> fullTrainSet = new ArrayList<>(dataset);
            for (int e = 0; e < bestEpoch; e++) {
                trainOneEpoch(fullTrainSet, bs, labelSmoothing);
                DatasetMetrics fullMetrics = evaluateDataset(fullTrainSet, this.decisionThreshold);
                double fullPrecision = (fullMetrics.tp + fullMetrics.fp) == 0 ? 0.0 : (double) fullMetrics.tp / (fullMetrics.tp + fullMetrics.fp);
                double fullRecall = (fullMetrics.tp + fullMetrics.fn) == 0 ? 0.0 : (double) fullMetrics.tp / (fullMetrics.tp + fullMetrics.fn);
                double fullF1 = (fullPrecision + fullRecall) == 0.0 ? 0.0 : 2.0 * fullPrecision * fullRecall / (fullPrecision + fullRecall);
                double fullFpr = (fullMetrics.fp + fullMetrics.tn) == 0 ? 0.0 : (double) fullMetrics.fp / (fullMetrics.fp + fullMetrics.tn);

                Logger.info(String.format(
                        "Final-fit Epoch %d/%d | Train [Loss: %.4f, Acc: %.1f%%] | Precision: %.4f | Recall: %.4f | F1: %.4f | FPR: %.4f",
                        (e + 1), bestEpoch, fullMetrics.avgLoss, fullMetrics.acc, fullPrecision, fullRecall, fullF1, fullFpr));
            }

            DatasetMetrics recalibrationMetrics = evaluateDataset(fullTrainSet, this.decisionThreshold);
            ThresholdMetrics recalibratedThreshold = selectBestThreshold(recalibrationMetrics.pairs, this.decisionThreshold);
            this.decisionThreshold = recalibratedThreshold.threshold;
            Logger.info(String.format(
                    "Final-fit threshold recalibrated -> %.2f | Precision: %.4f | Recall: %.4f | F1: %.4f | FPR: %.4f",
                    this.decisionThreshold, recalibratedThreshold.precision, recalibratedThreshold.recall,
                    recalibratedThreshold.f1, recalibratedThreshold.fpr));
            Logger.info(String.format(
                    "Final-fit complete -> threshold %.2f retained for inference.",
                    this.decisionThreshold));
        } else {
            this.decisionThreshold = initialDecisionThreshold;
        }
    }
}
