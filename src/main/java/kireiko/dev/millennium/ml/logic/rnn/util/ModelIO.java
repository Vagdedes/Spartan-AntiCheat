package kireiko.dev.millennium.ml.logic.rnn.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ModelIO {
    public static final int MAGIC = 0xDEAD0003;
    public static final int VERSION = 4;

    public static void writeArr(DataOutputStream out, double[] a) throws IOException {
        if (a == null) { out.writeInt(-1); return; }
        out.writeInt(a.length);
        for (double v : a) out.writeDouble(v);
    }

    public static double[] readArr(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0) return null;
        double[] a = new double[n];
        for (int i = 0; i < n; i++) a[i] = in.readDouble();
        return a;
    }
}
