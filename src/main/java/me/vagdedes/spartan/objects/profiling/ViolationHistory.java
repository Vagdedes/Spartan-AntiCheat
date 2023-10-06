package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ViolationHistory {

    private final ResearchEngine.DataType dataType;
    private final Enums.HackType hackType;
    private int days;

    private final Map<Integer, List<PlayerViolation>> violationsMap;
    private final List<PlayerViolation> violationsList;

    // Base of all object initiations
    public ViolationHistory(Enums.HackType hackType,
                            ResearchEngine.DataType dataType,
                            Map<Integer, List<PlayerViolation>> violationsData,
                            List<PlayerViolation> violationsMap,
                            int days) { // Constructor
        this.hackType = hackType;
        this.dataType = dataType;
        this.violationsMap = violationsData;
        this.violationsList = violationsMap;
        this.days = days;
    }

    // Used for object initiation in the player-profile object
    ViolationHistory(Enums.HackType hackType, ResearchEngine.DataType dataType) { // Local
        this(hackType, dataType, new ConcurrentHashMap<>(), new CopyOnWriteArrayList<>(), -1);
    }

    // Separator

    public void clear() {
        violationsMap.clear();
        violationsList.clear();
        clearDays();
    }

    public void clearDays() {
        days = -1;
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    private boolean isViolationDefaultImportant(int level, boolean def) {
        return def ? level >= hackType.getCheck().getDefaultCancelViolation() :
                level >= CancelViolation.get(hackType, dataType);
    }

    // Separator

    public void increaseViolations(PlayerViolation playerViolation) {
        int identity = playerViolation.divisionIdentity;
        List<PlayerViolation> list = violationsMap.get(identity);

        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            list.add(playerViolation);
            violationsMap.put(identity, list);
        } else {
            list.add(playerViolation);
        }
        violationsList.add(playerViolation);
    }

    // Separator

    public int getAllViolations() {
        return violationsList.size();
    }

    public int getImportantViolations(boolean def) {
        if (!violationsMap.isEmpty()) {
            int counter = 0;

            for (List<PlayerViolation> list : violationsMap.values()) {
                if (isViolationDefaultImportant(list.get(0).getLevel(), def)) {
                    counter += list.size();
                }
            }
            return counter;
        }
        return 0;
    }

    public int getUniqueViolations() {
        return violationsMap.size();
    }

    // Separator

    public Collection<Map.Entry<PlayerViolation, Integer>> getViolationCounts() {
        int violationsSize = violationsMap.size();

        if (violationsSize > 0) {
            Map<PlayerViolation, Integer> map = new LinkedHashMap<>(violationsSize);

            for (List<PlayerViolation> list : violationsMap.values()) {
                map.put(list.get(0), list.size());
            }
            return map.entrySet();
        }
        return new ArrayList<>(0);
    }

    public List<PlayerViolation> getViolationsList() {
        return new ArrayList<>(violationsList);
    }

    public Map<Integer, List<PlayerViolation>> getViolationsMap() {
        return new LinkedHashMap<>(violationsMap);
    }

    // Separator

    public Set<String> getDates() {
        int violationsSize = violationsMap.size();

        if (violationsSize > 0) {
            Set<String> dates = new HashSet<>(violationsSize);

            for (PlayerViolation playerViolation : violationsList) {
                dates.add(playerViolation.getDate());
            }
            return dates;
        }
        return new HashSet<>(0);
    }

    public int getDays() {
        return days == -1 ? days = getDates().size() : days;
    }
}
