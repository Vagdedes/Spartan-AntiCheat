package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.CharUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class InformationAnalysis {

    // Structure

    private static final Map<Integer, Map<Integer, Set<Integer>>>
            numbers = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));
    private static final Map<Integer, Boolean>
            detectionHasOption = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));

    private static boolean containsDetection(String string, String find) {
        int index = string.indexOf(find);

        if (index > -1) {
            int findLength = find.length(),
                    stringLength = string.length();

            // Start
            if (index == 0) {
                index += findLength;
                return index < stringLength && string.charAt(index) == '_';
            }

            // End
            if (index == (stringLength - 1)) {
                return string.charAt(index - 1) == '_';
            }

            // Between
            if (string.charAt(index - 1) == '_') {
                index += findLength;
                return index >= stringLength || string.charAt(index) == '_';
            }
        }
        return false;
    }

    private static String removeDetectionDetails(String detection) {
        detection = detection.split("\\(")[0];
        boolean foundNumberPreviously = false;
        StringBuilder reconstruct = new StringBuilder();

        for (int position = 0; position < detection.length(); position++) {
            char character = detection.charAt(position);

            if (CharUtils.isNumeric(character)) {
                foundNumberPreviously = true;
            } else {
                if (foundNumberPreviously && character == '.') { // Do not add character as it is part of a decimal
                    continue;
                }
                foundNumberPreviously = false;
                reconstruct.append(character);
            }
        }
        return reconstruct.toString();
    }

    // Orientation

    private final int key;
    final String detection;
    private final String[] array;

    InformationAnalysis(Enums.HackType hackType, String information) {
        this.array = information.split(" ");

        String detection = array[1];
        this.detection = detection.substring(0, detection.length() - 1); // Remove comma
        this.key = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();
    }

    boolean isOption(Enums.HackType hackType) {
        Collection<String> configKeys = hackType.getCheck().getOptionKeys();
        int configKeyCount = configKeys.size();

        if (configKeyCount > 0) {
            int hash = (key * SpartanBukkit.hashCodeMultiplier) + configKeyCount;

            synchronized (detectionHasOption) {
                Boolean cachedDetection = detectionHasOption.get(hash);

                if (cachedDetection != null) {
                    return cachedDetection;
                } else {
                    List<String> foundDetections = new ArrayList<>(configKeyCount);

                    for (String keyword : removeDetectionDetails(detection).split("-")) {
                        for (String configKey : configKeys) {
                            if (containsDetection(configKey, keyword)) {
                                foundDetections.add(configKey);
                            }
                        }
                    }

                    // Use the config key instead to cover all the sub-detections
                    if (foundDetections.size() == 1) {
                        detectionHasOption.put(hash, true);
                        return true;
                    } else {
                        detectionHasOption.put(hash, false);
                    }
                }
            }
        }
        return false;
    }

    Collection<Number> getNumbers() {
        int size = array.length;

        if (size >= 2) {
            synchronized (numbers) {
                Map<Integer, Set<Integer>> childMap = numbers.get(key);

                if (childMap == null) {
                    childMap = new LinkedHashMap<>();
                    Map<Integer, Number> positionsAndNumbers = identifyAndShowNumbers(array, size);
                    childMap.put(size, positionsAndNumbers.keySet());
                    numbers.put(key, childMap);
                    return positionsAndNumbers.values();
                } else {
                    Set<Integer> positions = childMap.get(size);

                    if (positions != null) {
                        return showNumbers(array, size, positions);
                    } else {
                        Map<Integer, Number> positionsAndNumbers = identifyAndShowNumbers(array, size);
                        childMap.put(size, positionsAndNumbers.keySet());
                        return positionsAndNumbers.values();
                    }
                }
            }
        } else {
            return new ArrayList<>(0);
        }
    }

    private Map<Integer, Number> identifyAndShowNumbers(String[] array, int size) {
        Map<Integer, Number> map = new LinkedHashMap<>(size);

        for (int position = 0; position < array.length; position++) {
            if (position % 2 == 1) {
                String string = array[position].substring(1); // Removes the comma
                Number number = AlgebraUtils.returnValidDecimal(string);

                if (number != null) {
                    map.put(position, number);
                } else {
                    number = AlgebraUtils.returnValidInteger(string);

                    if (number != null) {
                        map.put(position, number);
                    }
                }
            }
        }
        return map;
    }

    private Collection<Number> showNumbers(String[] array, int size, Set<Integer> positions) {
        List<Number> numbers = new ArrayList<>(size);

        for (int position : positions) {
            if (position < size) {
                String string = array[position].substring(1); // Removes the comma
                Number number = AlgebraUtils.returnValidDecimal(string);

                if (number != null) {
                    numbers.add(number);
                } else {
                    number = AlgebraUtils.returnValidInteger(string);

                    if (number != null) {
                        numbers.add(number);
                    }
                }
            } else {
                break;
            }
        }
        return numbers;
    }
}
