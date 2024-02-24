package com.vagdedes.spartan.utils.java;

import java.sql.Timestamp;

public class TimeUtils {
    
    public static final String dateSeparator = "/";
    public static final String timeSeparator = ":";

    public static String getTime(Timestamp s) {
        String string = s.toString();
        String hour = string.substring(11, 13);
        String minute = string.substring(14, 16);
        String second = string.substring(17, 19);
        return hour + timeSeparator + minute + timeSeparator + second;
    }

    public static String getDayMonthYear(Timestamp s) {
        String string = s.toString();
        String year = string.substring(0, 4);
        String month = string.substring(5, 7);
        String day = string.substring(8, 10);
        return day + dateSeparator + month + dateSeparator + year;
    }

    public static String getMonthDayYear(Timestamp s) {
        String string = s.toString();
        String year = string.substring(0, 4);
        String month = string.substring(5, 7);
        String day = string.substring(8, 10);
        return month + dateSeparator + day + dateSeparator + year;
    }

    public static String getYearMonthDay(Timestamp s) {
        String string = s.toString();
        String year = string.substring(0, 4);
        String month = string.substring(5, 7);
        String day = string.substring(8, 10);
        return year + dateSeparator + month + dateSeparator + day;
    }

    public static int getDifference(Timestamp t, int divide) {
        return (int) ((System.currentTimeMillis() - t.getTime()) / divide);
    }
}
