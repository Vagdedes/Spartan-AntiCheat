package me.vagdedes.spartan.utils.java;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

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

    public static Timestamp timestamp(int days, int hours, int minutes) {
        Date dt = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, days);
        c.add(Calendar.HOUR, hours);
        c.add(Calendar.MINUTE, minutes);
        dt = c.getTime();
        return new Timestamp(dt.getTime());
    }

    public static long getTimePassed(long l) {
        return System.currentTimeMillis() - l;
    }

    public static int getDifference(Timestamp t, int divide) {
        return (int) ((System.currentTimeMillis() - t.getTime()) / divide);
    }

    public static long getTime(int multiplier, char type) {
        long time = multiplier * 1000L;

        switch (type) {
            case 'm':
                return time * 60L;
            case 'h':
                return time * 60L * 60L;
            case 'd':
                return time * 60L * 60L * 24L;
            case 'w':
                return time * 60L * 60L * 24L * 7L;
            case 'y':
                return time * 60L * 60L * 24L * 365L;
            default:
                return 0L;
        }
    }
}
