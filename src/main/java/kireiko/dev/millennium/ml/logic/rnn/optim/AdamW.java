package kireiko.dev.millennium.ml.logic.rnn.optim;

public final class AdamW {
    private static final double B1 = 0.9;
    private static final double B2 = 0.999;
    private static final double EPS = 1e-8;

    public long t = 0;

    public void incrementStep() {
        t++;
    }

    public void step(double[] w, double[] g, double[] m, double[] v, double lr, double wd, double clip) {
        double bc1 = 1.0 - Math.pow(B1, t);
        double bc2 = 1.0 - Math.pow(B2, t);
        boolean doClip = clip > 0.0 && Double.isFinite(clip);

        for (int i = 0; i < w.length; i++) {
            double gi = g[i];
            if (Double.isNaN(gi) || Double.isInfinite(gi)) gi = 0.0;
            if (doClip) {
                if (gi > clip) gi = clip;
                else if (gi < -clip) gi = -clip;
            }

            w[i] -= lr * wd * w[i];

            m[i] = B1 * m[i] + (1.0 - B1) * gi;
            v[i] = B2 * v[i] + (1.0 - B2) * gi * gi;

            double mHat = m[i] / bc1;
            double vHat = v[i] / bc2;

            w[i] -= lr * mHat / (Math.sqrt(vHat) + EPS);
        }
    }

    public void stepScalar(DoubleRef w, double g, DoubleRef m, DoubleRef v, double lr, double wd, double clip) {
        double bc1 = 1.0 - Math.pow(B1, t);
        double bc2 = 1.0 - Math.pow(B2, t);
        boolean doClip = clip > 0.0 && Double.isFinite(clip);

        double gi = g;
        if (Double.isNaN(gi) || Double.isInfinite(gi)) gi = 0.0;
        if (doClip) {
            if (gi > clip) gi = clip;
            else if (gi < -clip) gi = -clip;
        }

        w.value -= lr * wd * w.value;

        m.value = B1 * m.value + (1.0 - B1) * gi;
        v.value = B2 * v.value + (1.0 - B2) * gi * gi;

        double mHat = m.value / bc1;
        double vHat = v.value / bc2;

        w.value -= lr * mHat / (Math.sqrt(vHat) + EPS);
    }

    public static final class DoubleRef {
        public double value;
        public DoubleRef(double v) { value = v; }
    }
}
