package com.vagdedes.spartan.utils.java.math;

import java.util.Objects;

public class TrigonometryUtils {

    public static final int triangleTeamDivision = AlgebraUtils.integerRound(Math.PI);

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

    public static class Side {

        public final double x, y;
        private int hash;

        public Side(double x, double y) {
            this.x = x;
            this.y = y;
            this.hash = 0;
        }

        public int hash() {
            return hash != 0 ? hash : (hash = Objects.hash(x, y));
        }

        public boolean equals(Side side) {
            return side.hash() == this.hash();
        }

        public double distanceX(double x) {
            return Math.abs(this.x - x);
        }

        public double distanceY(double y) {
            return Math.abs(this.y - y);
        }

        public double distanceXY() {
            return Math.abs(x - y);
        }

        public double distance(Side side) {
            return Math.sqrt(AlgebraUtils.getPreDistance(this.x, side.x) + AlgebraUtils.getPreDistance(this.y, side.y));
        }
    }

    public static class Triangle {

        public final double side1, side2, side3;
        private int hash;

        public Triangle(double side1, double side2, double side3) {
            this.side1 = side1;
            this.side2 = side2;
            this.side3 = side3;
            this.hash = 0;
        }

        public Triangle(Side side1, Side side2, Side side3) {
            this(side1.distanceXY(), side2.distanceXY(), side3.distanceXY());
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
                    AlgebraUtils.getPreDistance(this.side1, triangle.side1)
                            + AlgebraUtils.getPreDistance(this.side2, triangle.side2)
                            + AlgebraUtils.getPreDistance(this.side3, triangle.side3)
            );
        }

        public TriangleProperties getProperties() {
            return new TriangleProperties(this);
        }
    }

    public static class TriangleProperties {

        public final double[] ratios;
        public final int[] teams;

        private TriangleProperties(Triangle triangle) {
            double[] degrees = triangle.angleDegrees();

            if (triangle.isEuclidean(degrees)) {
                this.ratios = new double[3];
                this.teams = new int[3];
                double angleDegrees1 = degrees[0],
                        angleDegrees2 = degrees[1],
                        angleDegrees3 = degrees[2];
                ratios[0] = angleDegrees1 < angleDegrees2 ? angleDegrees1 / angleDegrees2 : angleDegrees2 / angleDegrees1;
                ratios[1] = angleDegrees1 < angleDegrees3 ? angleDegrees1 / angleDegrees3 : angleDegrees3 / angleDegrees1;
                ratios[2] = angleDegrees2 < angleDegrees3 ? angleDegrees2 / angleDegrees3 : angleDegrees3 / angleDegrees2;
                this.teams[0] = AlgebraUtils.floorToNearest(angleDegrees1, triangleTeamDivision);
                this.teams[1] = AlgebraUtils.floorToNearest(angleDegrees2, triangleTeamDivision);
                this.teams[2] = AlgebraUtils.floorToNearest(angleDegrees3, triangleTeamDivision);
            } else {
                this.ratios = new double[0];
                this.teams = new int[0];
            }
        }
    }
}
