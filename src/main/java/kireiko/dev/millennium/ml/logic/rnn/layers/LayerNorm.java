package kireiko.dev.millennium.ml.logic.rnn.layers;

public final class LayerNorm {
    private static final double EPS = 1e-5;

    public static final class Cache {
        public double mean;
        public double invStd;
        public double[] x;
        public double[] xHat;
    }

    public static double[] forward(double[] x, double[] gamma, double[] beta, int off, Cache c) {
        int n = x.length;
        double mean = 0.0;
        for (double v : x) mean += v;
        mean /= n;

        double var = 0.0;
        for (double v : x) {
            double d = v - mean;
            var += d * d;
        }
        var /= n;

        double invStd = 1.0 / Math.sqrt(var + EPS);

        double[] out = new double[n];
        double[] xHat = new double[n];
        for (int i = 0; i < n; i++) {
            xHat[i] = (x[i] - mean) * invStd;
            out[i] = gamma[off + i] * xHat[i] + beta[off + i];
        }

        if (c != null) {
            c.mean = mean;
            c.invStd = invStd;
            c.x = x;
            c.xHat = xHat;
        }
        return out;
    }

    public static double[] backward(double[] dY, double[] gamma, int off, Cache c, double[] dGamma, double[] dBeta) {
        int n = dY.length;

        double[] dXhat = new double[n];
        for (int i = 0; i < n; i++) {
            dGamma[off + i] += dY[i] * c.xHat[i];
            dBeta[off + i] += dY[i];
            dXhat[i] = dY[i] * gamma[off + i];
        }

        double sumDXhat = 0.0;
        double sumDXhatXhat = 0.0;
        for (int i = 0; i < n; i++) {
            sumDXhat += dXhat[i];
            sumDXhatXhat += dXhat[i] * c.xHat[i];
        }

        double[] dX = new double[n];
        double invN = 1.0 / n;
        for (int i = 0; i < n; i++) {
            dX[i] = c.invStd * (dXhat[i] - invN * sumDXhat - c.xHat[i] * invN * sumDXhatXhat);
        }
        return dX;
    }
}
