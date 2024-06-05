package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InformationAnalysis {

    private static final int max = 16_384;
    private static final Map<Integer, StoredInformation> map = new ConcurrentHashMap<>(2, 1.0f);

    // Separator

    private static final class StoredInformation {

        private final Map<Integer, Set<Integer>> positions;
        private final boolean isOption;

        private StoredInformation(boolean isOption) {
            this.positions = new LinkedHashMap<>();
            this.isOption = isOption;
        }
    }

    // Orientation

    final int hash;
    public final int identity;
    final String detection;
    final boolean isOption;
    final Collection<Number> numbers;

    public InformationAnalysis(Enums.HackType hackType, String information) {
        String[] array = information.split(" ");

        if (array.length == 1) {
            this.detection = array[0];
            this.hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();
            this.isOption = false;
            this.numbers = new ArrayList<>(0);
            this.identity = this.hash;
        } else {
            String detection = array[1];
            this.detection = detection.substring(0, detection.length() - 1); // Remove comma
            this.hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

            // Separator

            Collection<String> configKeys = hackType.getCheck().getOptionKeys();
            int configKeyCount = configKeys.size();
            StoredInformation data = map.get(this.hash);

            if (data == null) {
                List<String> foundDetections = new ArrayList<>(configKeyCount);

                for (String keyword : this.removeDetectionDetails(detection).split("-")) {
                    for (String configKey : configKeys) {
                        if (this.containsDetection(configKey, keyword)) {
                            foundDetections.add(configKey);
                        }
                    }
                }

                data = new StoredInformation(foundDetections.size() == 1);
                Map<Integer, Number> positionsAndNumbers = this.identifyAndShowNumbers(array, array.length);
                data.positions.put(array.length, positionsAndNumbers.keySet());
                this.numbers = positionsAndNumbers.values();
                int delete = map.size() - max;

                if (delete > 0) {
                    Iterator<Integer> iterator = map.keySet().iterator();

                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();

                        if (--delete == 0) {
                            break;
                        }
                    }
                }
                map.put(this.hash, data);
            } else {
                Set<Integer> positions = data.positions.get(array.length);

                if (positions != null) {
                    this.numbers = this.showNumbers(array, array.length, positions);
                } else {
                    Map<Integer, Number> positionsAndNumbers = this.identifyAndShowNumbers(array, array.length);
                    data.positions.put(array.length, positionsAndNumbers.keySet());
                    this.numbers = positionsAndNumbers.values();
                }
            }
            this.isOption = data.isOption;

            // Separator

            int identity = this.hash;

            if (!this.numbers.isEmpty()) {
                for (Number number : this.numbers) {
                    if (number instanceof Double) {
                        identity = (identity * SpartanBukkit.hashCodeMultiplier)
                                + Double.hashCode(AlgebraUtils.cut(number.doubleValue(), MovementProcessing.quantumPrecision));
                    } else {
                        identity = (identity * SpartanBukkit.hashCodeMultiplier) + number.intValue();
                    }
                }
            }
            this.identity = identity;
        }
    }

    // Separator

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

    // Separator

    private boolean containsDetection(String string, String find) {
        int index = string.indexOf(find);

        if (index > -1) {
            if (index == 0) { // Start
                index += find.length();
                return index < string.length() && string.charAt(index) == '_';
            } else {
                int stringLength = string.length();

                if (index == (stringLength - 1)) { // End
                    return string.charAt(index - 1) == '_';
                } else if (string.charAt(index - 1) == '_') { // Between
                    index += find.length();
                    return index >= stringLength || string.charAt(index) == '_';
                }
            }
        }
        return false;
    }

    private String removeDetectionDetails(String detection) {
        detection = detection.split("\\(")[0];
        boolean foundNumberPreviously = false;
        StringBuilder reconstruct = new StringBuilder();

        for (int position = 0; position < detection.length(); position++) {
            char character = detection.charAt(position);

            if (StringUtils.isNumeric(character)) {
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

}
