package com.vagdedes.spartan.utils.java.math;


import java.util.Collection;

public class LinearRegression {

    private static <E> double[] collectionToArray(Collection<E> collection) {
        double[] array = new double[collection.size()];
        int counter = 0;

        for (Object dbl : collection) {
            array[counter] = (double) dbl;
            counter++;
        }
        return array;
    }

    public final double intercept, slope, r2, error;
    public final int observations;

    // y = b0 + b1 * x
    // b0 = intercept
    // b1 = slope
    // x = predictor

    public LinearRegression(double[] x, double[] y) {
        int n = x.length;

        if (n != y.length) {
            throw new IllegalArgumentException("Array lengths must be equal to each other");
        }
        observations = n;
        double meanY = 0.0,
                meanX = 0.0;

        // First: Calculate summaries & means
        for (int i = 0; i < n; i++) {
            meanY += y[i];
            meanX += x[i];
        }
        meanY /= n;
        meanX /= n;

        // Second: Find slope
        double xMinusBarX_sumPowered = 0.0,
                yMinusBarY_MultipliedBy_xMinusBarX_sum = 0.0;

        for (int i = 0; i < n; i++) {
            double yMinusBarY = y[i] - meanY,
                    xMinusBarX = x[i] - meanX;
            yMinusBarY_MultipliedBy_xMinusBarX_sum += yMinusBarY * xMinusBarX;
            xMinusBarX_sumPowered += xMinusBarX * xMinusBarX;
        }
        this.slope = yMinusBarY_MultipliedBy_xMinusBarX_sum / xMinusBarX_sumPowered;
        this.intercept = meanY - (this.slope * meanX);

        // Third: Find R-Squared
        double yPredictedMinusBarY_sumPowered = 0.0,
                yMinusBarY_sumPowered = 0.0,
                yPredictedMinusY_sumPowered = 0.0;

        for (int i = 0; i < n; i++) {
            double yCurrent = y[i],
                    yPredicted = predictY(x[i]),
                    yPredictedMinusY = yPredicted - yCurrent,
                    yPredictedMinusBarY = yPredicted - meanY,
                    yMinusBarY = yCurrent - meanY;
            yPredictedMinusBarY_sumPowered += yPredictedMinusBarY * yPredictedMinusBarY;
            yMinusBarY_sumPowered += yMinusBarY * yMinusBarY;
            yPredictedMinusY_sumPowered += yPredictedMinusY * yPredictedMinusY;
        }
        this.r2 = yPredictedMinusBarY_sumPowered / yMinusBarY_sumPowered;
        this.error = Math.sqrt(yPredictedMinusY_sumPowered / (n - 2));
    }

    public <E> LinearRegression(Collection<E> x, Collection<E> y) {
        this(collectionToArray(x), collectionToArray(y));
    }

    public double predictY(double x) {
        return intercept + (slope * x);
    }
}
