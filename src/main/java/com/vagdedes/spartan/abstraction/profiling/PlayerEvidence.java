package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    private final PlayerProfile parent;
    private final Map<Enums.HackType, String> historical, informational; // Live object is used for synchronization

    PlayerEvidence(PlayerProfile profile) {
        this.parent = profile;
        this.historical = new ConcurrentHashMap<>(2, 1.0f);
        this.informational = new ConcurrentHashMap<>(2, 1.0f);
    }

    // Separator

    public void clear(boolean historical, boolean informational) {
        if (historical) {
            this.historical.clear();
        }
        if (informational) {
            this.informational.clear();
        }
    }

    public void remove(Enums.HackType hackType, boolean historical, boolean informational) {
        if (historical) {
            this.historical.remove(hackType);
        }
        if (informational) {
            this.informational.remove(hackType);
        }
    }

    public void add(Enums.HackType hackType, String info,
                    boolean historical,
                    boolean informational) {
        if (historical) {
            this.historical.put(hackType, info);
        }
        if (informational) {
            this.informational.put(hackType, info);
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList(boolean informational) {
        Collection<Enums.HackType> set = new HashSet<>(this.historical.keySet());
        SpartanPlayer player = this.parent.getSpartanPlayer();

        if (player != null) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                if (!set.contains(hackType)
                        && player.getViolations(hackType).getSuspicionRatio() != -1.0) {
                    set.add(hackType);
                }
            }
        }
        if (informational) {
            set.addAll(this.informational.keySet());
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, String>> getKnowledgeEntries(boolean informational) {
        Set<Map.Entry<Enums.HackType, String>> set = new HashSet<>(this.historical.entrySet());
        SpartanPlayer player = this.parent.getSpartanPlayer();

        if (player != null) {
            Map<Enums.HackType, String> map = new LinkedHashMap<>();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                if (!this.historical.containsKey(hackType)) {
                    double suspicion = player.getViolations(hackType).getSuspicionRatio();

                    if (suspicion != -1.0) {
                        map.put(hackType, "Ratio: " + suspicion);
                    }
                }
            }

            if (!map.isEmpty()) {
                set.addAll(map.entrySet());
            }
        }

        if (informational) {
            set.addAll(this.informational.entrySet());
        }
        return set;
    }

}
