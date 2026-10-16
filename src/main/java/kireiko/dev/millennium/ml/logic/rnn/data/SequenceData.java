package kireiko.dev.millennium.ml.logic.rnn.data;

public final class SequenceData {
    public final double[][] x;
    public final double[] mask;

    public SequenceData(double[][] x, double[] mask) {
        this.x = x;
        this.mask = mask;
    }

    public int length() {
        return x.length;
    }
}
