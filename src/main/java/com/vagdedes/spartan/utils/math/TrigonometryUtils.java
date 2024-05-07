package com.vagdedes.spartan.utils.math;

import java.util.Objects;

public class TrigonometryUtils {

    // Angle (a + A = Pair, B = Angle)
    public static double sineAngle(double a, double A, double b) {
        return Math.asin(Math.sin(A) * b / a);
    }

    // Side (a + A = Pair, b = side)
    public static double sineSide(double a, double A, double B) {
        return Math.sin(B) * a / Math.sin(A);
    }

    // Angle (C)
    public static double cosineAngleC(double a, double b, double c) {
        return Math.acos(((b * b) + (a * a) - (c * c)) / (2.0 * b * a));
    }

    // Side (c)
    public static double cosineSide(double a, double b, double C) {
        return Math.sqrt((a * a) + (b * b) - 2.0 * a * b * Math.cos(C));
    }

    private static double triangleArea(double a, double b, double c) {
        double p = (a + b + c) / 2.0;
        return Math.sqrt(p * (p - a) * (p - b) * (p - c));
    }

    // Separator

    public static class Triangle {

        public final double side1, side2, side3;
        private int hash;

        public Triangle(double side1, double side2, double side3) {
            this.side1 = side1;
            this.side2 = side2;
            this.side3 = side3;
            this.hash = 0;
        }

        public int hash() {
            return hash != 0 ? hash : (hash = Objects.hash(side1, side2, side3));
        }

        public boolean equals(Triangle triangle) {
            return triangle.hash() == this.hash();
        }

        public boolean isEuclidean(double[] radiansOrDegrees) {
            double radians1 = radiansOrDegrees[0];

            if (!Double.isNaN(radians1) && !Double.isInfinite(radians1)) {
                double radians2 = radiansOrDegrees[1];
                return !Double.isNaN(radians2) && !Double.isInfinite(radians2);
            }
            return false;
        }

        public double[] sides() {
            return new double[]{side1, side2, side3};
        }

        public double[] angleRadians() {
            double[] list = new double[3];
            double angleC = cosineAngleC(side1, side2, side3),
                    angleB = sineAngle(side3, angleC, side2);
            list[0] = angleC;
            list[1] = angleB;
            list[2] = Math.PI - angleC - angleB;
            return list;
        }

        public double[] angleDegrees() {
            double[] list = new double[3];
            double angle1 = cosineAngleC(side1, side2, side3),
                    angle2 = sineAngle(side1, angle1, side2);
            list[0] = Math.toDegrees(angle1);
            list[1] = Math.toDegrees(angle2);
            list[2] = 180.0 - list[0] - list[1];
            return list;
        }

        public double area() {
            return triangleArea(side1, side2, side3);
        }

        public double length() {
            return side1 + side2 + side3;
        }

        public double distance(Triangle triangle) {
            return Math.sqrt(
                    AlgebraUtils.getSquare(this.side1, triangle.side1)
                            + AlgebraUtils.getSquare(this.side2, triangle.side2)
                            + AlgebraUtils.getSquare(this.side3, triangle.side3)
            );
        }
    }
}
