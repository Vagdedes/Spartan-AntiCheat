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

    public static String convertMilliseconds(long milliseconds) {
        final long millisecondsPerSecond = 1000;
        final long millisecondsPerMinute = millisecondsPerSecond * 60;
        final long millisecondsPerHour = millisecondsPerMinute * 60;
        final long millisecondsPerDay = millisecondsPerHour * 24;
        final long millisecondsPerWeek = millisecondsPerDay * 7;

        // Average time calculations
        final double daysPerMonth = 30.44;
        final double daysPerYear = 365.25;
        final long millisecondsPerMonth = (long) (millisecondsPerDay * daysPerMonth);
        final long millisecondsPerYear = (long) (millisecondsPerDay * daysPerYear);

        long remainingMilliseconds = milliseconds;

        long years = remainingMilliseconds / millisecondsPerYear;
        remainingMilliseconds %= millisecondsPerYear;

        long months = remainingMilliseconds / millisecondsPerMonth;
        remainingMilliseconds %= millisecondsPerMonth;

        long weeks = remainingMilliseconds / millisecondsPerWeek;
        remainingMilliseconds %= millisecondsPerWeek;

        long days = remainingMilliseconds / millisecondsPerDay;
        remainingMilliseconds %= millisecondsPerDay;

        long hours = remainingMilliseconds / millisecondsPerHour;
        remainingMilliseconds %= millisecondsPerHour;

        long minutes = remainingMilliseconds / millisecondsPerMinute;
        remainingMilliseconds %= millisecondsPerMinute;

        long seconds = remainingMilliseconds / millisecondsPerSecond;

        StringBuilder timeBuilder = new StringBuilder();

        if (years > 0) timeBuilder.append(years).append("y ");
        if (months > 0) timeBuilder.append(months).append("m ");
        if (weeks > 0) timeBuilder.append(weeks).append("w ");
        if (days > 0) timeBuilder.append(days).append("d ");
        if (hours > 0) timeBuilder.append(hours).append("h ");
        if (minutes > 0) timeBuilder.append(minutes).append("m ");
        if (seconds > 0 || timeBuilder.length() == 0)
            timeBuilder.append(seconds).append("s ");

        return timeBuilder.toString().trim();
    }

}
