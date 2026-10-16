package kireiko.dev.millennium.math;

import com.google.common.collect.Lists;
import kireiko.dev.millennium.types.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Statistics {

    public static final double EXPANDER = Math.pow(2, 24);

    public static double getVariance(final Collection<? extends Number> data) {
        int count = 0;

        double sum = 0.0;
        double variance = 0.0;

        final double average;

        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }

        average = sum / count;

        for (final Number number : data) {
            variance += Math.pow(number.doubleValue() - average, 2.0);
        }

        return variance / count;
    }

    public static double getMin(final Collection<? extends Number> collection) {
        double min = Double.MAX_VALUE;

        for (final Number number : collection) {
            min = Math.min(min, number.doubleValue());
        }

        return min;
    }

    public static float getGCD(double s) {
        float f1 = (float) ((float) s * 0.6 + 0.2);
        return f1 * f1 * f1 * 8.0F;
    }

    public static float getGCDValue(double s) {
        return getGCD(s) * 0.15F;
    }

    public static double getMax(final Collection<? extends Number> collection) {
        // Double.MIN_VALUE is the smallest positive value, so it cannot be the starting point for negative numbers
        double max = Double.NEGATIVE_INFINITY;

        for (final Number number : collection) {
            max = Math.max(max, number.doubleValue());
        }

        return max == Double.NEGATIVE_INFINITY ? Double.MIN_VALUE : max;
    }

    public static double getStandardDeviation(final Collection<? extends Number> data) {
        final double variance = getVariance(data);

        return Math.sqrt(variance);
    }

    public static double getSkewness(final Collection<? extends Number> data) {
        double sum = 0;
        int count = 0;

        final List<Double> numbers = Lists.newArrayList();

        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;

            numbers.add(number.doubleValue());
        }

        Collections.sort(numbers);

        final double mean = sum / count;
        final double median = (count % 2 != 0) ? numbers.get(count / 2) : (numbers.get((count - 1) / 2) + numbers.get(count / 2)) / 2;
        final double variance = getVariance(data);

        return 3 * (mean - median) / variance;
    }

    public static double getAverage(final Collection<? extends Number> data) {
        double sum = 0.0;

        for (final Number number : data) {
            sum += number.doubleValue();
        }
        final double result = sum / data.size();
        return (Double.isNaN(result)) ? 0 : result;
    }

    public static double getKurtosis(final Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;

        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }

        if (count < 3.0) {
            return 0.0;
        }

        final double efficiencyFirst = count * (count + 1.0) / ((count - 1.0) * (count - 2.0) * (count - 3.0));
        final double efficiencySecond = 3.0 * Math.pow(count - 1.0, 2.0) / ((count - 2.0) * (count - 3.0));
        final double average = sum / count;

        double variance = 0.0;
        double varianceSquared = 0.0;

        for (final Number number : data) {
            variance += Math.pow(average - number.doubleValue(), 2.0);
            varianceSquared += Math.pow(average - number.doubleValue(), 4.0);
        }

        return efficiencyFirst * (varianceSquared / Math.pow(variance / sum, 2.0)) - efficiencySecond;
    }

    public static long getMode(final Collection<? extends Number> array) {
        long mode = (long) array.toArray()[0];
        long maxCount = 0;

        for (final Number value : array) {
            int count = 1;

            for (final Number i : array) {
                if (i.equals(value))
                    count++;

                if (count > maxCount) {
                    mode = (long) value;
                    maxCount = count;
                }
            }
        }

        return mode;
    }


    public static float distanceBetweenAngles(final float alpha, final float beta) {
        final float alphaX = alpha % 360;
        final float betaX = beta % 360;
        final float delta = Math.abs(alphaX - betaX);

        return (float) Math.abs(Math.min(360.0 - delta, delta));
    }

    public static double getModeDouble(final Double[] data) {
        double maxValue = -1.0d;
        int maxCount = 0;

        for (int i = 0; i < data.length; ++i) {
            final double currentValue = data[i];
            int currentCount = 1;

            for (int j = i + 1; j < data.length; ++j) {
                if (Math.abs(data[j] - currentValue) < 0.001) {
                    ++currentCount;
                }
            }

            if (currentCount > maxCount) {
                maxCount = currentCount;
                maxValue = currentValue;
            } else if (currentCount == maxCount) {
                maxValue = Double.NaN;
            }
        }

        return maxValue;
    }

    public static double getMedian(final List<Double> data) {
        if (data.size() % 2 == 0) {
            return (data.get(data.size() / 2) + data.get(data.size() / 2 - 1)) / 2;
        } else {
            return data.get(data.size() / 2);
        }
    }

    public static boolean isExponentiallySmall(final Number number) {
        return number.doubleValue() < 1 && (Double.toString(number.doubleValue()).contains("E") || number.doubleValue() == 0.0);
    }

    public static boolean isExponentiallyLarge(final Number number) {
        return number.doubleValue() > 10000 && Double.toString(number.doubleValue()).contains("E");
    }

    public static long getGcd(final long current, final long previous) {
        return (previous <= 16384L) ? current : getGcd(previous, current % previous);
    }

    public static double getGcd(final double a, final double b) {
        if (a == b) return 0;
        if (a < b) {
            return getGcd(b, a);
        }

        if (Math.abs(b) < 0.00001) {
            return a;
        } else {
            return getGcd(b, a - Math.floor(a / b) * b);
        }
    }

    public static long getAbsoluteGcd(final float current, final float last) {
        final long currentExpanded = (long) (current * EXPANDER);
        final long lastExpanded = (long) (last * EXPANDER);

        return getGcd(currentExpanded, lastExpanded);
    }

    public static long getAbsoluteGcd(final double current, final double last) {
        final long currentExpanded = (long) (current * EXPANDER);
        final long lastExpanded = (long) (last * EXPANDER);

        return getGcd(currentExpanded, lastExpanded);
    }

    public static float gcdRational(final List<Float> numbers) {
        float result = numbers.get(0);

        for (int i = 1; i < numbers.size(); i++) {
            result = gcdRational(numbers.get(i), result);

            if (result < 1.0E-7) { //This usually means that the GCD is beyond the precision we can handle
                return 0;
            }
        }

        return result;
    }

    public static float gcdRational(final float a, final float b) {
        if (a == 0) {
            return b;
        }

        final int quotient = getIntQuotient(b, a);

        float remainder = ((b / a) - quotient) * a;

        if (Math.abs(remainder) < Math.max(a, b) * 1.0E-3F) {
            remainder = 0;
        }

        return gcdRational(remainder, a);
    }

    public static int getIntQuotient(final float dividend, final float divisor) {
        final float ans = dividend / divisor;
        final float error = Math.max(dividend, divisor) * 1.0E-3F;

        return (int) (ans + error);
    }

    public static double getCps(final Collection<? extends Number> data) {
        return 20 / getAverage(data);
    }

    public static int getDuplicates(final Collection<? extends Number> data) {
        return data.size() - getDistinct(data);
    }

    /**
     * @param - The collection of numbers you want analyze
     * @return - A pair of the high and low outliers
     * @See - https://en.wikipedia.org/wiki/Outlier
     */
    public static Pair<List<Double>, List<Double>> getOutliers(final Collection<? extends Number> collection) {
        final List<Double> values = new ArrayList<>();

        for (final Number number : collection) {
            values.add(number.doubleValue());
        }

        final double q1 = getMedian(values.subList(0, values.size() / 2));
        final double q3 = getMedian(values.subList(values.size() / 2, values.size()));

        final double iqr = Math.abs(q1 - q3);
        final double lowThreshold = q1 - 1.5 * iqr, highThreshold = q3 + 1.5 * iqr;

        final Pair<List<Double>, List<Double>> tuple = new Pair<>(new ArrayList<>(), new ArrayList<>());

        for (final Double value : values) {
            if (value < lowThreshold) {
                tuple.getX().add(value);
            } else if (value > highThreshold) {
                tuple.getY().add(value);
            }
        }

        return tuple;
    }

    public static List<Long> convertToLongList(List<Integer> integerList) {
        List<Long> longList = new ArrayList<>();
        for (Integer i : integerList) {
            if (i != null) {
                longList.add(i.longValue());
            } else {
                longList.add(null);
            }
        }
        return longList;
    }

    public static double getKireikoGeneric(final Collection<? extends Number> collection) {
        return (Statistics.getKurtosis(collection)
                + (Statistics.getVariance(collection)
                * 3.0)) / 20.0;
    }

    public static double calculatePercentile(final Collection<? extends Number> data, double percentile) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Collection cannot be empty");
        }
        List<Double> sortedValues = data.stream()
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sortedValues.size()) index = sortedValues.size() - 1;

        return sortedValues.get(index);
    }

    public static double getCoefficientOfVariation(final Collection<? extends Number> data) {
        double mean = getAverage(data);
        return mean == 0 ? 0 : getStandardDeviation(data) / mean;
    }

    public static double getPearsonCorrelation(final List<? extends Number> x, final List<? extends Number> y) {
        if (x.size() != y.size() || x.isEmpty()) return 0.0;

        double meanX = getAverage(x);
        double meanY = getAverage(y);

        double sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < x.size(); i++) {
            double dx = x.get(i).doubleValue() - meanX;
            double dy = y.get(i).doubleValue() - meanY;
            sumXY += dx * dy;
            sumX2 += dx * dx;
            sumY2 += dy * dy;
        }

        return sumX2 == 0 || sumY2 == 0 ? 0 : sumXY / Math.sqrt(sumX2 * sumY2);
    }

    public static double getZScore(final double value, final Collection<? extends Number> data) {
        double mean = getAverage(data);
        double stdDev = getStandardDeviation(data);
        return stdDev == 0 ? 0 : (value - mean) / stdDev;
    }

    public static double getShannonEntropy(final Collection<? extends Number> data) {
        Map<Double, Long> freqMap = data.stream()
                .collect(Collectors.groupingBy(Number::doubleValue, Collectors.counting()));

        double total = data.size();
        return -freqMap.values().stream()
                .mapToDouble(count -> (count / total) * (Math.log(count / total) / Math.log(2)))
                .sum();
    }

    public static double getHarmonicMean(final Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;

        for (Number number : data) {
            if (number.doubleValue() != 0) {
                sum += 1.0 / number.doubleValue();
                count++;
            }
        }

        return count == 0 ? 0 : count / sum;
    }

    public static double getLinearTrend(final List<? extends Number> data) {
        int n = data.size();
        if (n < 2) return 0;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = data.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    public static double getCovariance(final List<? extends Number> x, final List<? extends Number> y) {
        if (x.size() != y.size() || x.isEmpty()) return 0.0;

        double meanX = getAverage(x);
        double meanY = getAverage(y);

        double sum = 0;
        for (int i = 0; i < x.size(); i++) {
            sum += (x.get(i).doubleValue() - meanX) * (y.get(i).doubleValue() - meanY);
        }

        return sum / x.size();
    }

    public static double getSharpeRatio(final Collection<? extends Number> returns, double riskFreeRate) {
        double mean = getAverage(returns);
        double stdDev = getStandardDeviation(returns);
        return stdDev == 0 ? 0 : (mean - riskFreeRate) / stdDev;
    }

    public static double getQuantile(final Collection<? extends Number> data, double quantile) {
        List<Double> sorted = data.stream().map(Number::doubleValue).sorted().collect(Collectors.toList());
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public static double getGiniIndex(final Collection<? extends Number> data) {
        List<Double> sorted = data.stream().map(Number::doubleValue).sorted().collect(Collectors.toList());
        int n = sorted.size();
        double sum = 0;
        for (Double aDouble : sorted) {
            for (int j = 0; j < n; j++) {
                sum += Math.abs(aDouble - sorted.get(j));
            }
        }

        double mean = getAverage(data);
        return mean == 0 ? 0 : sum / (2 * n * n * mean);
    }

    public static double roundToPlace(double value, int places) {
        double multiplier = Math.pow(10, places);
        return Math.round(value * multiplier) / multiplier;
    }

    public static List<Double> getZScoreOutliers(final Collection<? extends Number> data, double threshold) {
        List<Double> outliers = new ArrayList<>();
        double mean = getAverage(data);
        double stdDev = getStandardDeviation(data);

        for (Number number : data) {
            double zScore = (number.doubleValue() - mean) / stdDev;
            if (Math.abs(zScore) > threshold) {
                outliers.add(number.doubleValue());
            }
        }

        return outliers;
    }

    public static double getRSquared(final List<? extends Number> actual, final List<? extends Number> predicted) {
        if (actual.size() != predicted.size() || actual.isEmpty()) return 0.0;

        double meanActual = getAverage(actual);
        double ssTotal = 0, ssResidual = 0;

        for (int i = 0; i < actual.size(); i++) {
            double diffActual = actual.get(i).doubleValue() - meanActual;
            double diffResidual = actual.get(i).doubleValue() - predicted.get(i).doubleValue();
            ssTotal += diffActual * diffActual;
            ssResidual += diffResidual * diffResidual;
        }

        return ssTotal == 0 ? 0 : 1 - (ssResidual / ssTotal);
    }

    private static double pearsonCorrelation(List<? extends Number> x, List<? extends Number> y) {
        double meanX = getAverage(x);
        double meanY = getAverage(y);
        double numerator = 0.0, denominatorX = 0.0, denominatorY = 0.0;

        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i).doubleValue() - meanX;
            double diffY = y.get(i).doubleValue() - meanY;
            numerator += diffX * diffY;
            denominatorX += diffX * diffX;
            denominatorY += diffY * diffY;
        }

        return numerator / Math.sqrt(denominatorX * denominatorY);
    }

    public static double spearmanCorrelation(List<? extends Number> x, List<? extends Number> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            throw new IllegalArgumentException("Lists must be of the same size and non-empty");
        }

        List<Double> rankX = getRanks(x);
        List<Double> rankY = getRanks(y);

        return pearsonCorrelation(rankX, rankY);
    }

    public static double rSquared(List<? extends Number> actual, List<? extends Number> predicted) {
        if (actual.size() != predicted.size() || actual.isEmpty()) {
            throw new IllegalArgumentException("Lists must be of the same size and non-empty");
        }

        double meanActual = getAverage(actual);
        double ssTotal = 0.0, ssResidual = 0.0;

        for (int i = 0; i < actual.size(); i++) {
            double y = actual.get(i).doubleValue();
            double yPred = predicted.get(i).doubleValue();
            ssTotal += Math.pow(y - meanActual, 2);
            ssResidual += Math.pow(y - yPred, 2);
        }

        return 1 - (ssResidual / ssTotal);
    }

    public static double tTest(List<? extends Number> sample1, List<? extends Number> sample2) {
        double mean1 = getAverage(sample1);
        double mean2 = getAverage(sample2);
        double var1 = getVariance(sample1);
        double var2 = getVariance(sample2);

        int n1 = sample1.size();
        int n2 = sample2.size();

        return (mean1 - mean2) / Math.sqrt((var1 / n1) + (var2 / n2));
    }

    public static List<Double> movingAverage(List<? extends Number> data, int windowSize) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i <= data.size() - windowSize; i++) {
            double sum = 0;
            for (int j = 0; j < windowSize; j++) {
                sum += data.get(i + j).doubleValue();
            }
            result.add(sum / windowSize);
        }
        return result;
    }

    public static List<Double> exponentialMovingAverage(List<? extends Number> data, double smoothingFactor) {
        List<Double> result = new ArrayList<>();
        double ema = data.get(0).doubleValue();
        result.add(ema);
        for (int i = 1; i < data.size(); i++) {
            double value = data.get(i).doubleValue();
            ema = smoothingFactor * value + (1 - smoothingFactor) * ema;
            result.add(ema);
        }
        return result;
    }

    public static double[] linearRegression(List<? extends Number> x, List<? extends Number> y) {
        int n = x.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = x.get(i).doubleValue();
            double yi = y.get(i).doubleValue();
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    public static double regressionStandardError(
            List<? extends Number> x, List<? extends Number> y,
            double slope, double intercept) {
        int n = x.size();
        double sumSquaredErrors = 0;
        for (int i = 0; i < n; i++) {
            double xi = x.get(i).doubleValue();
            double yi = y.get(i).doubleValue();
            double predicted = slope * xi + intercept;
            sumSquaredErrors += Math.pow(yi - predicted, 2);
        }
        return Math.sqrt(sumSquaredErrors / (n - 2));
    }

    public static double getWinsorizedMean(List<? extends Number> data, double trimFraction) {
        List<Double> sorted = data.stream()
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());
        int n = sorted.size();
        int trimCount = (int) (n * trimFraction);
        double lowerBound = sorted.get(trimCount - 1);
        double upperBound = sorted.get(n - trimCount);
        List<Double> winsorized = new ArrayList<>(n);
        for (double value : sorted) {
            if (value < lowerBound) {
                winsorized.add(lowerBound);
            } else if (value > upperBound) {
                winsorized.add(upperBound);
            } else {
                winsorized.add(value);
            }
        }
        return winsorized.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public static double kolmogorovSmirnovTest(final List<? extends Number> data, Function<Double, Double> cdfFunction) {
        List<Double> sorted = data.stream().map(Number::doubleValue).sorted().collect(Collectors.toList());
        int n = sorted.size();
        double dStatistic = 0;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (i + 1) / (double) n;
            double theoreticalCDF = cdfFunction.apply(sorted.get(i));
            dStatistic = Math.max(dStatistic, Math.abs(empiricalCDF - theoreticalCDF));
        }
        return dStatistic;
    }

    public static double getExponentiallyWeightedVariance(final List<? extends Number> data, double alpha) {
        double ewMean = data.get(0).doubleValue();
        double ewVariance = 0;
        for (int i = 1; i < data.size(); i++) {
            double value = data.get(i).doubleValue();
            ewMean = alpha * value + (1 - alpha) * ewMean;
            ewVariance = alpha * Math.pow(value - ewMean, 2) + (1 - alpha) * ewVariance;
        }
        return ewVariance;
    }

    public static double calculateAimSuspicionIndex(final List<? extends Number> aimDeltas,
                                                    final List<? extends Number> reactionTimes) {
        double avgDelta = Statistics.getAverage(aimDeltas);
        double stdDelta = Statistics.getStandardDeviation(aimDeltas);
        double deltaZSum = 0;
        for (Number delta : aimDeltas) {
            double z = stdDelta == 0 ? 0 : (delta.doubleValue() - avgDelta) / stdDelta;
            deltaZSum += Math.abs(z);
        }
        double avgDeltaZ = deltaZSum / aimDeltas.size();

        double avgReaction = Statistics.getAverage(reactionTimes);
        double stdReaction = Statistics.getStandardDeviation(reactionTimes);
        double reactionZSum = 0;
        for (Number rt : reactionTimes) {
            double z = stdReaction == 0 ? 0 : (rt.doubleValue() - avgReaction) / stdReaction;
            reactionZSum += Math.abs(z);
        }
        double avgReactionZ = reactionZSum / reactionTimes.size();
        return avgDeltaZ * 0.6 + avgReactionZ * 0.4;
    }

    public static List<Double> kalmanFilterPredict(final List<? extends Number> measurements,
                                                   double processVariance, double measurementVariance) {
        int n = measurements.size();
        List<Double> predictions = new ArrayList<>(n);
        double estimate = measurements.get(0).doubleValue();
        double errorCovariance = 1;

        predictions.add(estimate);

        for (int i = 1; i < n; i++) {
            double prediction = estimate;
            double predictedError = errorCovariance + processVariance;
            double measurement = measurements.get(i).doubleValue();
            double kalmanGain = predictedError / (predictedError + measurementVariance);
            estimate = prediction + kalmanGain * (measurement - prediction);
            errorCovariance = (1 - kalmanGain) * predictedError;

            predictions.add(estimate);
        }

        return predictions;
    }

    public static double dynamicTimeWarpingDistance(final List<Double> series1, final List<Double> series2) {
        int n = series1.size();
        int m = series2.size();
        double[][] dtw = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dtw[i], Double.POSITIVE_INFINITY);
        }
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double cost = Math.abs(series1.get(i - 1) - series2.get(j - 1));
                dtw[i][j] = cost + Math.min(
                        Math.min(dtw[i - 1][j],
                                dtw[i][j - 1]),
                        dtw[i - 1][j - 1]);
            }
        }
        return dtw[n][m];
    }

    public static List<Integer> cusumDetection(final List<? extends Number> data, double threshold, double drift) {
        List<Double> series = data.stream().map(Number::doubleValue).collect(Collectors.toList());
        List<Integer> changePoints = new ArrayList<>();
        double posSum = 0;
        double negSum = 0;
        for (int i = 0; i < series.size(); i++) {
            double value = series.get(i);
            posSum = Math.max(0, posSum + value - drift);
            negSum = Math.min(0, negSum + value + drift);

            if (posSum > threshold) {
                changePoints.add(i);
                posSum = 0;
            } else if (Math.abs(negSum) > threshold) {
                changePoints.add(i);
                negSum = 0;
            }
        }
        return changePoints;
    }

    public static List<Float> getJiffDelta(List<? extends Number> data, int depth) {
        List<Float> result = new ArrayList<>();
        for (Number n : data) result.add(n.floatValue());
        for (int i = 0; i < depth; i++) {
            List<Float> calculate = new ArrayList<>();
            float old = Float.MIN_VALUE;
            for (float n : result) {
                if (old == Float.MIN_VALUE) {
                    old = n;
                    continue;
                }
                calculate.add(Math.abs(Math.abs(n) - Math.abs(old)));
                old = n;
            }
            result = new ArrayList<>(calculate);
        }
        return result;
    }

    public static List<Double> getRanks(List<? extends Number> data) {
        List<Double> sorted = data.stream().map(Number::doubleValue).sorted().collect(Collectors.toList());
        return data.stream().map(v -> (double) (sorted.indexOf(v.doubleValue()) + 1)).collect(Collectors.toList());
    }

    public static int getDistinct(final Collection<? extends Number> data) {
        return (int) data.stream().distinct().count();
    }

    public static double hypot(final double a, final double b) {
        return Math.sqrt(a * a + b * b);
    }

    public static double getIQR(final Collection<? extends Number> data) {
        List<Double> sorted = data.stream().map(Number::doubleValue).sorted().collect(Collectors.toList());
        return calculatePercentile(sorted, 75) - calculatePercentile(sorted, 25);
    }
}
