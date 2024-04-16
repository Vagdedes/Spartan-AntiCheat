package com.vagdedes.spartan.utils.java;

import java.sql.Timestamp;

public class TimeUtils {

    public static final String dateSeparator = "/";
    private static final String timeSeparator = ":";

    private static String getTime(String string) {
        String hour = string.substring(11, 13);
        String minute = string.substring(14, 16);
        String second = string.substring(17, 19);
        return hour + timeSeparator + minute + timeSeparator + second;
    }

    private static String getYearMonthDay(String string) {
        String year = string.substring(0, 4);
        String month = string.substring(5, 7);
        String day = string.substring(8, 10);
        return year + dateSeparator + month + dateSeparator + day;
    }

    public static String getTime(Timestamp s) {
        return getTime(s.toString());
    }

    public static String getYearMonthDay(Timestamp s) {
        return getYearMonthDay(s.toString());
    }

    public static String[] getAll(Timestamp s) {
        String string = s.toString();
        return new String[]{
                getYearMonthDay(string),
                getTime(string)
        };
    }

    public static int getDifference(Timestamp t, int divide) {
        return (int) ((System.currentTimeMillis() - t.getTime()) / divide);
    }
}
