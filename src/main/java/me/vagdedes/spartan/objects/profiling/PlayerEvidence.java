package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    enum EvidenceType {
        Hacker, Suspected, Legitimate;

        private final int count;

        EvidenceType() {
            switch (this.ordinal()) {
                case 0:
                    count = 3;
                    break;
                case 1:
                    count = 1;
                    break;
                default:
                    count = 0;
                    break;
            }
        }
    }

    // Object

    private final Map<Enums.HackType, String> evidence;
    private EvidenceType type;
    private boolean noCalculations;

    PlayerEvidence() {
        this.type = EvidenceType.Legitimate;
        this.evidence = new LinkedHashMap<>(Enums.hackTypeLength);
        this.noCalculations = true;
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList() {
        return new HashSet<>(evidence.keySet());
    }

    public Set<Map.Entry<Enums.HackType, String>> getKnowledgeEntry() {
        return new HashSet<>(evidence.entrySet());
    }

    public String getKnowledge(Enums.HackType hackType, String color) {
        String knowledge = evidence.get(hackType);
        return color == null || knowledge == null ? knowledge : addColor(knowledge, color);
    }

    // Separator

    public String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    public int getCount() {
        return evidence.size();
    }

    public EvidenceType getType() {
        return type;
    }

    boolean noCalculations() {
        return noCalculations;
    }

    // Separator

    Collection<Enums.HackType> has(EvidenceType type) {
        return this.type == type ? evidence.keySet() : new ArrayList<>(0);
    }

    public boolean has(Enums.HackType hackType) {
        return evidence.containsKey(hackType);
    }

    // Separator

    boolean startCalculation(boolean bool) {
        if (bool) {
            this.noCalculations = false;
            return true;
        }
        return false;
    }

    void add(Enums.HackType hackType, String reason) {
        this.evidence.put(hackType, reason);
    }

    public void remove(Enums.HackType hackType) {
        this.evidence.remove(hackType);
    }

    void clear() {
        evidence.clear();
        this.type = EvidenceType.Legitimate;
    }

    EvidenceType judge() {
        int count = evidence.size();

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.count) {
                this.type = evidenceType;
                return evidenceType;
            }
        }
        return EvidenceType.Legitimate;
    }
}
