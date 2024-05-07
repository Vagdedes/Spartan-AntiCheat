package com.vagdedes.spartan.utils.math;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MatrixUtils {

    public static double getSimilarity(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            int max, min;

            if (array1.length > array2.length) {
                max = array1.length;
                min = array2.length;
            } else {
                max = array2.length;
                min = array1.length;
                double[] smallArray = array1;
                array1 = array2;
                array2 = smallArray;
            }
            int rest = max % min;

            if (rest == 0) {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            Arrays.copyOfRange(array1, i * min, i * min + min),
                            array2
                    );
                }
                return similarity / (double) chunks;
            } else {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            Arrays.copyOfRange(array1, i * min, i * min + min),
                            array2
                    );
                }
                similarity = similarity(
                        Arrays.copyOfRange(array1, chunks * min, chunks * min + rest),
                        array2
                );
                return similarity / (chunks + 1.0);
            }
        } else {
            return perfectSimilarity(array1, array2);
        }
    }

    private static double perfectSimilarity(double[] array1, double[] array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;

        for (int i = 0; i < array1.length; i++) {
            dotProduct += array1[i] * array2[i];
            magnitude1 += array1[i] * array1[i];
            magnitude2 += array2[i] * array2[i];
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private static double similarity(double[] array1, double[] array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;

        for (int i = 0; i < Math.min(array1.length, array2.length); i++) {
            dotProduct += array1[i] * array2[i];
            magnitude1 += array1[i] * array1[i];
            magnitude2 += array2[i] * array2[i];
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    // Separator

    public static double getSimilarity(float[] array1, float[] array2) {
        if (array1.length != array2.length) {
            int max, min;

            if (array1.length > array2.length) {
                max = array1.length;
                min = array2.length;
            } else {
                max = array2.length;
                min = array1.length;
                float[] smallArray = array1;
                array1 = array2;
                array2 = smallArray;
            }
            int rest = max % min;

            if (rest == 0) {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            Arrays.copyOfRange(array1, i * min, i * min + min),
                            array2
                    );
                }
                return similarity / (double) chunks;
            } else {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            Arrays.copyOfRange(array1, i * min, i * min + min),
                            array2
                    );
                }
                similarity = similarity(
                        Arrays.copyOfRange(array1, chunks * min, chunks * min + rest),
                        array2
                );
                return similarity / (chunks + 1.0);
            }
        } else {
            return perfectSimilarity(array1, array2);
        }
    }

    private static double perfectSimilarity(float[] array1, float[] array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;

        for (int i = 0; i < array1.length; i++) {
            dotProduct += array1[i] * array2[i];
            magnitude1 += array1[i] * array1[i];
            magnitude2 += array2[i] * array2[i];
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private static double similarity(float[] array1, float[] array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;

        for (int i = 0; i < Math.min(array1.length, array2.length); i++) {
            dotProduct += array1[i] * array2[i];
            magnitude1 += array1[i] * array1[i];
            magnitude2 += array2[i] * array2[i];
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    // Separator

    public static double getSimilarity(List<? extends Number> array1, List<? extends Number> array2) {
        if (array1.size() != array2.size()) {
            int max, min;

            if (array1.size() > array2.size()) {
                max = array1.size();
                min = array2.size();
            } else {
                max = array2.size();
                min = array1.size();
                List<? extends Number> smallArray = array1;
                array1 = array2;
                array2 = smallArray;
            }
            int rest = max % min;

            if (rest == 0) {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            array1.subList(i * min, i * min + min),
                            array2
                    );
                }
                return similarity / (double) chunks;
            } else {
                double similarity = 0.0;
                int chunks = max / min;

                for (int i = 0; i < chunks; i++) {
                    similarity += perfectSimilarity(
                            array1.subList(i * min, i * min + min),
                            array2
                    );
                }
                similarity = similarity(
                        array1.subList(chunks * min, chunks * min + rest),
                        array2
                );
                return similarity / (chunks + 1.0);
            }
        } else {
            return perfectSimilarity(array1, array2);
        }
    }

    private static double perfectSimilarity(List<? extends Number> array1, List<? extends Number> array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;
        Iterator<? extends Number> iterator1 = array1.iterator();
        Iterator<? extends Number> iterator2 = array2.iterator();

        while (iterator1.hasNext()) {
            double one = iterator1.next().doubleValue(),
                    two = iterator2.next().doubleValue();
            dotProduct += one * two;
            magnitude1 += one * one;
            magnitude2 += two * two;
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private static double similarity(List<? extends Number> array1, List<? extends Number> array2) {
        // Calculate dot product
        double dotProduct = 0.0,
                magnitude1 = 0.0,
                magnitude2 = 0.0;
        Iterator<? extends Number> iterator1 = array1.iterator();
        Iterator<? extends Number> iterator2 = array2.iterator();

        for (int i = 0; i < Math.min(array1.size(), array2.size()); i++) {
            double one = iterator1.next().doubleValue(),
                    two = iterator2.next().doubleValue();
            dotProduct += one * two;
            magnitude1 += one * one;
            magnitude2 += two * two;
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

}
