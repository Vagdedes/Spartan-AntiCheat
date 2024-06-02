package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    public enum EvidenceType {
        LEGITIMATE, SUSPECTED, HACKER;
        private final String string;

        EvidenceType() {
            switch (this.ordinal()) {
                case 0:
                    this.string = "Legitimate";
                    break;
                case 1:
                    this.string = "Suspected";
                    break;
                default:
                    this.string = "Hacker";
                    break;

            }
        }

        @Override
        public String toString() {
            return string;
        }
    }

    // Object

    private EvidenceType type;
    final Map<Enums.HackType, String> live, historical, informational; // Live object is used for synchronization

    PlayerEvidence() {
        this.type = EvidenceType.LEGITIMATE;
        this.live = Collections.synchronizedMap(new LinkedHashMap<>(Enums.HackType.values().length));
        this.historical = new LinkedHashMap<>(Enums.HackType.values().length);
        this.informational = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    // Separator

    public EvidenceType getType() {
        return type;
    }

    // Separator

    public void clear(boolean live,
                      boolean historical,
                      boolean informational,
                      boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.clear();
            }
            if (historical) {
                this.historical.clear();
            }
            if (informational) {
                this.informational.clear();
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    public void remove(Enums.HackType hackType,
                       boolean live,
                       boolean historical,
                       boolean informational,
                       boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.remove(hackType);
            }
            if (historical) {
                this.historical.remove(hackType);
            }
            if (informational) {
                this.informational.remove(hackType);
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    public void add(Enums.HackType hackType, String info,
                    boolean live,
                    boolean historical,
                    boolean informational,
                    boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.put(hackType, info);
            }
            if (historical) {
                this.historical.put(hackType, info);
            }
            if (informational) {
                this.informational.put(hackType, info);
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList(boolean informational) {
        synchronized (this.live) {
            return getRawKnowledgeList(informational);
        }
    }

    private Collection<Enums.HackType> getRawKnowledgeList(boolean informational) {
        Collection<Enums.HackType> set = new HashSet<>(live.keySet());
        set.addAll(historical.keySet());

        if (informational) {
            set.addAll(this.informational.keySet());
        }
        return set;
    }

    // Separator

    public String getKnowledge(Enums.HackType hackType, String color, boolean informational) {
        synchronized (this.live) {
            String knowledge = historical.get(hackType);

            if (knowledge == null) {
                knowledge = live.get(hackType);

                if (knowledge == null && informational) {
                    knowledge = this.informational.get(hackType);
                }
            }
            return knowledge == null ? null : addColor(knowledge, color);
        }
    }

    private String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    boolean has(EvidenceType type) {
        return this.type == type;
    }

    public boolean has(Enums.HackType hackType, boolean informational) {
        synchronized (this.live) {
            return live.containsKey(hackType)
                    || historical.containsKey(hackType)
                    || informational && this.informational.containsKey(hackType);
        }
    }

    // Separator

    public Collection<Enums.HackType> calculate(SpartanPlayer player, PlayerViolation playerViolation) {
        synchronized (this.live) {
            if (this.live.containsKey(playerViolation.hackType)
                    || this.historical.containsKey(playerViolation.hackType)) {
                return this.getRawKnowledgeList(false);
            }
        }
        Check check = playerViolation.hackType.getCheck();

        if (check.supportsLiveEvidence) {
            double ignoredViolations = playerViolation.getIgnoredViolations(player),
                    violationCount = player.getViolations(playerViolation.hackType).getLevel(playerViolation.identity)
                            - AlgebraUtils.integerCeil(Latency.getDelay(player))
                            - ignoredViolations;

            if (violationCount > 0.0) {
                double ratio = violationCount / ignoredViolations;

                if (ratio >= Check.standardIgnoredViolations) {
                    synchronized (this.live) {
                        this.live.put(playerViolation.hackType,
                                "Ratio: " + AlgebraUtils.cut(ratio, 2) + "%"
                        );
                        this.judgeLocal();
                    }
                    InteractiveInventory.playerInfo.refresh(player.name);
                    MainMenu.refresh();
                    return this.getRawKnowledgeList(false);
                }
            }
        }
        return new ArrayList<>(0);
    }

    // Separator

    void judgeLocal() {
        int count = this.getRawKnowledgeList(false).size();
        EvidenceType type = this.type;

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.ordinal()) {
                type = evidenceType;
            }
        }
        this.type = type;
    }

    public void judge() {
        synchronized (this.live) {
            this.judgeLocal();
        }
    }

}
