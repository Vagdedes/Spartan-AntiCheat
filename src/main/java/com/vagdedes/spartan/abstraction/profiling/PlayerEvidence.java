package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.performance.CancelViolation;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    public static final double standardRatio = 3.5;

    enum EvidenceType {
        Hacker(2),
        Suspected(1),
        Legitimate(0);

        private final int requirement;

        EvidenceType(int requirement) {
            this.requirement = requirement;
        }
    }

    // Object

    private final PlayerProfile profile;
    private EvidenceType type;
    final Map<Enums.HackType, String> live; // Live object is used for synchronization
    public final Map<Enums.HackType, String> historical; // Live object is used for synchronization

    PlayerEvidence(PlayerProfile profile) {
        this.profile = profile;
        this.type = EvidenceType.Legitimate;
        this.live = Collections.synchronizedMap(new LinkedHashMap<>(Enums.HackType.values().length));
        this.historical = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    // Separator

    public EvidenceType getType() {
        return type;
    }

    // Separator

    public void clear(boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.clear();
            }
            if (historical) {
                this.historical.clear();
            }
            if (judge) {
                judgeLocal();
            }
        }
    }

    public void remove(Enums.HackType hackType, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.remove(hackType);
            }
            if (historical) {
                this.historical.remove(hackType);
            }
            if (judge) {
                judgeLocal();
            }
        }
    }

    public void add(Enums.HackType hackType, String info, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.put(hackType, info);
            }
            if (historical) {
                this.historical.put(hackType, info);
            }
            if (judge) {
                judgeLocal();
            }
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList() {
        synchronized (this.live) {
            return getRawKnowledgeList();
        }
    }

    private Collection<Enums.HackType> getRawKnowledgeList() {
        Collection<Enums.HackType> set = new HashSet<>(live.keySet());
        set.addAll(historical.keySet());
        return set;
    }

    // Separator

    public String getKnowledge(Enums.HackType hackType, String color) {
        synchronized (this.live) {
            String knowledge = historical.get(hackType);

            if (knowledge == null) {
                knowledge = live.get(hackType);
            }
            return knowledge == null ? knowledge : addColor(knowledge, color);
        }
    }

    private String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    boolean has(EvidenceType type) {
        return this.type == type;
    }

    public boolean has(Enums.HackType hackType) {
        synchronized (this.live) {
            return live.containsKey(hackType) || historical.containsKey(hackType);
        }
    }

    // Separator

    public Collection<Enums.HackType> calculate(SpartanPlayer player, Enums.HackType hackType) {
        synchronized (this.live) {
            if (this.live.containsKey(hackType)
                    || this.historical.containsKey(hackType)) {
                return getRawKnowledgeList();
            } else {
                Check check = hackType.getCheck();

                if (check.supportsLiveEvidence) {
                    int violationCount = player.getViolations(hackType).getLevel()
                            - CancelViolation.get(hackType, profile.getDataType())
                            - AlgebraUtils.integerCeil(Latency.getDelay(player));

                    if (violationCount > 0) {
                        double ratio = (violationCount / ((double) CancelViolation.get(hackType, profile.getDataType())));

                        if (ratio >= standardRatio) {
                            this.live.put(hackType,
                                    "Ratio: " + AlgebraUtils.cut(ratio, 2) + "%"
                            );
                            this.judgeLocal();
                            InteractiveInventory.playerInfo.refresh(player.name);
                            MainMenu.refresh();
                            return getRawKnowledgeList();
                        }
                    }
                }
                return new ArrayList<>(0);
            }
        }
    }

    // Separator

    void judgeLocal() {
        int count = this.getRawKnowledgeList().size();
        EvidenceType type = this.type;

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.requirement) {
                type = evidenceType;

                if (evidenceType != EvidenceType.Legitimate) {
                    break;
                }
            }
        }
        this.type = type;
    }

    public void judge() {
        synchronized (this.live) {
            judgeLocal();
        }
    }
}
