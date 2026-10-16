package kireiko.dev.millennium.ml.logic.rnn.data.preprocessing;

import kireiko.dev.millennium.ml.logic.rnn.data.SequenceData;

public interface SequencePreprocessor {
    SequenceData prepare(double[][] rawVecs);
}