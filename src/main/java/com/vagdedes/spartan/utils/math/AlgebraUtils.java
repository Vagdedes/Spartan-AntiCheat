package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Random;

public class AlgebraUtils {

    public static final double SQUARE_ROOT_2 = Math.sqrt(2.0);

    public static int integerFloor(double d) {
        return NumberConversions.floor(d);
    }

    public static int integerRound(double d) {
        return NumberConversions.round(d);
    }

    public static int integerCeil(double d) {
        return NumberConversions.ceil(d);
    }

    public static double integersToDouble(int a, int b) {
        return a + (b == 0 ? 0 : b / Math.pow(10, 1 + (int) Math.log10(b)));
    }

    // Separator

    public static double cut(double value, int cut) {
        cut = (int) Math.pow(10, cut);
        return Math.floor(value * cut) / (double) cut;
    }

    public static float cut(float value, int cut) {
        cut = (int) Math.pow(10, cut);
        return (float) (Math.floor(value * cut) / (double) cut);
    }

    // Separator

    public static double removeBase(double d) {
        return d - Math.floor(d);
    }

    // Separator

    public static double roundToPortion(double dbl, double portion) {
        return Math.floor(dbl * portion) / portion;
    }

    public static double ceilToPortion(double dbl, double portion) {
        return Math.ceil(dbl * portion) / portion;
    }

    public static double floorToPortion(double dbl, double portion) {
        return Math.floor(dbl * portion) / portion;
    }

    // Separator

    public static int roundToNearest(Number number, int nearest) {
        return (int) (nearest * Math.round(number.doubleValue() / (double) nearest));
    }

    public static int floorToNearest(Number number, int nearest) {
        return (int) (nearest * Math.floor(number.doubleValue() / (double) nearest));
    }

    public static int ceilToNearest(Number number, int nearest) {
        return (int) (nearest * Math.ceil(number.doubleValue() / (double) nearest));
    }

    // Separator

    public static int getUniqueMultiplications(int m) {
        int total = 0, loop = m - 1;

        for (int i = 0; i < loop; i++) {
            m--;
            total += m;
        }
        return total;
    }

    public static int getUniqueMultiplicationsInRow(int m, int row) {
        int loop = m - 1;

        for (int i = 0; i < loop; i++) {
            m--;

            if (i == row) {
                return m;
            }
        }
        return 0;
    }

    // Separator

    public static double getSquare(double d1, double d2) {
        double d = d1 - d2;
        return d * d;
    }

    public static double getSquaredDistance(double x1, double x2, double y1, double y2, double z1, double z2) {
        return getSquare(x1, x2) + getSquare(y1, y2) + getSquare(z1, z2);
    }

    public static double getDistance(double x1, double x2, double y1, double y2, double z1, double z2) {
        return Math.sqrt(getSquaredDistance(x1, x2, y1, y2, z1, z2));
    }

    // Separator

    public static int generalize(double number, int level) {
        return integerCeil(number * level);
    }

    // Separator

    public static double findGCD(double limit, double first, double second) {
        return second <= limit ? first : findGCD(limit, second, first % second);
    }

    // Separator

    public static double getHorizontalDistance(Location loc, Vector vec) {
        return Math.sqrt(getSquare(loc.getX(), vec.getX()) + getSquare(loc.getZ(), vec.getZ()));
    }

    public static double getHorizontalDistance(Vector vec, Location loc) {
        return getHorizontalDistance(loc, vec);
    }

    public static double getHorizontalDistance(SpartanLocation loc, Vector vec) {
        return Math.sqrt(getSquare(loc.getX(), vec.getX()) + getSquare(loc.getZ(), vec.getZ()));
    }

    public static double getHorizontalDistance(Vector vec, SpartanLocation loc) {
        return getHorizontalDistance(loc, vec);
    }

    public static double getHorizontalDistance(Location loc1, SpartanLocation loc2) {
        return Math.sqrt(getSquare(loc1.getX(), loc2.getX()) + getSquare(loc1.getZ(), loc2.getZ()));
    }

    public static double getHorizontalDistance(SpartanLocation loc1, Location loc2) {
        return getHorizontalDistance(loc2, loc1);
    }

    public static double getHorizontalDistance(Vector vec1, Vector vec2) {
        return Math.sqrt(getSquare(vec1.getX(), vec2.getX()) + getSquare(vec1.getZ(), vec2.getZ()));
    }

    public static double getHorizontalDistance(Location loc1, Location loc2) {
        return Math.sqrt(getSquare(loc1.getX(), loc2.getX()) + getSquare(loc1.getZ(), loc2.getZ()));
    }

    public static double getHorizontalDistance(SpartanLocation loc1, SpartanLocation loc2) {
        loc1.retrieveDataFrom(loc2);
        return Math.sqrt(getSquare(loc1.getX(), loc2.getX()) + getSquare(loc1.getZ(), loc2.getZ()));
    }

    // Separator

    public static double getVerticalDistance(Location loc1, SpartanLocation loc2) {
        return Math.sqrt(getSquare(loc1.getY(), loc2.getY()));
    }

    public static double getVerticalDistance(SpartanLocation loc1, Location loc2) {
        return getVerticalDistance(loc2, loc1);
    }

    public static double getVerticalDistance(Location loc1, Location loc2) {
        return Math.sqrt(getSquare(loc1.getY(), loc2.getY()));
    }

    public static double getVerticalDistance(SpartanLocation loc1, SpartanLocation loc2) {
        loc1.retrieveDataFrom(loc2);
        return Math.sqrt(getSquare(loc1.getY(), loc2.getY()));
    }

    // Separator

    public static float wrapAngleTo180(float value) {
        value %= 360.0f;

        if (value >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    public static double wrapAngleTo180(double value) {
        value %= 360.0;

        if (value >= 180.0) {
            value -= 360.0;
        }
        if (value < -180.0) {
            value += 360.0;
        }
        return value;
    }

    // Separator

    public static int randomInteger(int one, int two) {
        return new Random().nextInt(Math.abs(two - one)) + one;
    }

    public static double randomDouble(double one, double two) {
        return one + (two - one) * new Random().nextDouble();
    }

    public static float randomFloat(float one, float two) {
        return one + (two - one) * new Random().nextFloat();
    }

    // Separator

    public static Integer returnValidInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean validInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean validLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Double returnValidDecimal(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean validDecimal(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Float returnValidFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean validFloat(String str) {
        try {
            Float.parseFloat(str);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean validNumber(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        int start = chars[0] == '-' ? 1 : 0;
        int i;
        if (sz > start + 1 && chars[start] == '0' && chars[start + 1] == 'x') {
            i = start + 2;
            if (i == sz) {
                return false;
            } else {
                while (i < chars.length) {
                    if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }

                    ++i;
                }

                return true;
            }
        } else {
            --sz;

            for (i = start; i < sz || i < sz + 1 && allowSigns && !foundDigit; ++i) {
                if (chars[i] >= '0' && chars[i] <= '9') {
                    foundDigit = true;
                    allowSigns = false;
                } else if (chars[i] == '.') {
                    if (hasDecPoint || hasExp) {
                        return false;
                    }

                    hasDecPoint = true;
                } else if (chars[i] != 'e' && chars[i] != 'E') {
                    if (chars[i] != '+' && chars[i] != '-') {
                        return false;
                    }

                    if (!allowSigns) {
                        return false;
                    }

                    allowSigns = false;
                    foundDigit = false;
                } else {
                    if (hasExp) {
                        return false;
                    }

                    if (!foundDigit) {
                        return false;
                    }

                    hasExp = true;
                    allowSigns = true;
                }
            }

            if (i < chars.length) {
                if (chars[i] >= '0' && chars[i] <= '9') {
                    return true;
                } else if (chars[i] != 'e' && chars[i] != 'E') {
                    if (chars[i] == '.') {
                        return !hasDecPoint && !hasExp && foundDigit;
                    } else if (allowSigns || chars[i] != 'd' && chars[i] != 'D' && chars[i] != 'f' && chars[i] != 'F') {
                        if (chars[i] != 'l' && chars[i] != 'L') {
                            return false;
                        } else {
                            return foundDigit && !hasExp && !hasDecPoint;
                        }
                    } else {
                        return foundDigit;
                    }
                } else {
                    return false;
                }
            } else {
                return !allowSigns && foundDigit;
            }
        }
    }

    public static boolean isExponentVisible(double value) {
        return value < 1e-04;
    }

    // Separator

    public static double floatDouble(double d) {
        return (float) d;
    }
}
