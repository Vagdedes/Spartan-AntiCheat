package com.vagdedes.spartan.utils.java;

import org.bukkit.ChatColor;

import java.nio.charset.Charset;
import java.util.*;

public class StringUtils {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int idealDescriptionLimit = 40;

    public static String newStringUtf8(byte[] var) {
        return new String(var, UTF_8);
    }

    public static String decodeBase64(String s) {
        try {
            return StringUtils.newStringUtf8(Base64.getDecoder().decode(s));
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    public static String encodeBase64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    // Separator

    public static String replaceBars(final String s, final String color) {
        return (color == null ? "" : "§" + color) + s.replace("_", " ");
    }

    public static String replaceBars2(final String s, final String color) {
        return (color == null ? "" : "§" + color) + s.replace("_", "-");
    }

    public static String replaceSpaces(final String s) {
        return getClearColorString(s.replace(" ", "_"));
    }

    public static String reverseBars(final String s) {
        return getClearColorString(s.replace("-", "_"));
    }

    // Separator

    public static String getClearColorString(final String s) {
        return ChatColor.stripColor(s);
    }

    public static String getClearColorSyntaxString(final String s) {
        return s.replaceAll("&[a-z,0-9]", "");
    }

    // Separator

    public static String getMultipleArgs(String[] args, int start) {
        final StringBuilder s = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            s.append(args[i] + " ");
        }
        return s.substring(0, s.length() - 1);
    }

    public static List<String> constructDescription(String string, List<String> array, boolean space) {
        if (array == null) {
            array = new ArrayList<>(idealDescriptionLimit);
        }

        if (!string.isEmpty()) {
            String color = string.startsWith("§") ? string.substring(0, 2) : "";
            boolean hasColor = !color.isEmpty();

            if (hasColor) {
                string = string.substring(2);
            }

            if (!string.isEmpty()) {
                if (space) {
                    array.add("");
                }
                int level = 0;
                int add = idealDescriptionLimit + (hasColor ? 2 : 0);

                while (level <= string.length()) {
                    array.add(color + substring(string, level, level + add));
                    level += add;
                }
            }
        }
        return array;
    }

    public static String toString(Object[] a, String separator) {
        int iMax = a.length - 1;

        if (iMax == -1) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                continue;
            }
            b.append(a[i]);

            if (i == iMax) {
                return b.toString();
            }
            b.append(separator);
        }
        return b.toString();
    }

    public static <E> String toString(Collection<E> a, String separator) {
        int iMax = a.size() - 1;

        if (iMax == -1) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        int i = 0;

        for (Object o : a) {
            if (o == null) {
                continue;
            }
            b.append(o);

            if (i == iMax) {
                break;
            }
            b.append(separator);
            i++;
        }
        return b.toString();
    }

    // Separator

    public static String rearrange(String s) {
        char[] list = s.toCharArray();
        Arrays.sort(list);
        return new String(list);
    }

    public static String substring(String string, int start, int end) {
        int length = string.length();
        return start > length || start > end || start == end ? string : end > length ? string.substring(start, length) : string.substring(start, end);
    }

    public static String escapeMetaCharacters(String inputString) {
        final String[] metaCharacters = {"\\", "^", "$", "{", "}", "[", "]", "(", ")", ".", "*", "+", "?", "|", "<", ">", "-", "&", "%"};

        for (String metaCharacter : metaCharacters) {
            if (inputString.contains(metaCharacter)) {
                inputString = inputString.replace(metaCharacter, "\\" + metaCharacter);
            }
        }
        return inputString;
    }

    // Separator

    public static boolean stringContainsPartOfArray(String[] array, String storedKey) {
        for (String toFind : array) {
            if (storedKey.contains(toFind)) {
                return true;
            }
        }
        return false;
    }

    public static int countMatches(String str, String sub) {
        int count = 0;

        for (int idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub.length()) {
            count++;
        }
        return count;
    }

    // Separator

    public static boolean isNumeric(char c) {
        return c == '0'
                || c == '1'
                || c == '2'
                || c == '3'
                || c == '4'
                || c == '5'
                || c == '6'
                || c == '7'
                || c == '8'
                || c == '9';
    }
}
