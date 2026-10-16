package kireiko.dev.millennium.ml.logic.rnn.pooling;

public interface PoolingStrategy {
    int outputSize();
    double[] forward(double[][] hTime, double[] mask, PoolingCache cache);
    double[][] backward(double[][] hTime, double[] mask, double[] dPooled, PoolingCache cache, PoolingGrad gAcc);
}
