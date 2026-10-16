package kireiko.dev.millennium.ml.logic.rnn.util;

public final class MathOps {
    private MathOps() {}

    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    public static double sigmoid(double x) {
        x = clamp(x, -500.0, 500.0);
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static double[] xavierUniform(java.util.Random rng, int size, int fanIn, int fanOut) {
        double[] arr = new double[size];
        double limit = Math.sqrt(6.0 / (fanIn + fanOut));
        for (int i = 0; i < size; i++) arr[i] = (rng.nextDouble() * 2.0 - 1.0) * limit;
        return arr;
    }

    public static double[] orthogonal(java.util.Random rng, int n) {
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) m[i][j] = rng.nextGaussian();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                double dot = 0.0;
                double norm = 0.0;
                for (int k = 0; k < n; k++) {
                    dot += m[i][k] * m[j][k];
                    norm += m[j][k] * m[j][k];
                }
                if (norm > 1e-10) {
                    double s = dot / norm;
                    for (int k = 0; k < n; k++) m[i][k] -= s * m[j][k];
                }
            }
            double norm = 0.0;
            for (int k = 0; k < n; k++) norm += m[i][k] * m[i][k];
            norm = Math.sqrt(norm);
            if (norm > 1e-10) for (int k = 0; k < n; k++) m[i][k] /= norm;
        }

        double[] flat = new double[n * n];
        for (int i = 0; i < n; i++) System.arraycopy(m[i], 0, flat, i * n, n);
        return flat;
    }

    public static void addScaled(double[] dst, double[] src, double s) {
        for (int i = 0; i < dst.length; i++) dst[i] += src[i] * s;
    }

    public static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
