package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternStorage {

    private static final Collection<PatternStorage> instances = Collections.synchronizedList(new ArrayList<>());

    public static void consider(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                instance.considerLocal(profile);
            }
        }
    }

    public static void block(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                instance.blockLocal(profile);
            }
        }
    }

    // Separator

    private final String key;
    private final int[] generalizations;
    private final boolean generalization;
    private final Map<Integer, Collection<PatternValue>> available, unavailable;
    private final Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations;
    private final Set<PlayerProfile> blocked;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;
        this.blocked = Collections.synchronizedSet(new HashSet<>());

        this.generalizations = generalizations;
        this.generalization = generalizations.length > 0;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());

        this.available = Collections.synchronizedMap(new LinkedHashMap<>());
        this.available.put(0, new ArrayList<>());

        this.unavailable = Collections.synchronizedMap(new LinkedHashMap<>());
        this.unavailable.put(0, new ArrayList<>());

        for (int generalization : generalizations) {
            this.available.put(generalization, new ArrayList<>());
            this.unavailable.put(generalization, new ArrayList<>());
        }
        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        // todo
    }

    public void load() {
        // todo
    }

    // Separator

    private void rawCache(Map<Integer, Collection<PatternValue>> map,
                          SpartanPlayer player, PatternValue value, int generalization) {
        map.get(generalization).add(value);
        this.correlations.computeIfAbsent(
                player.getProfile(),
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                generalization,
                k -> new ArrayList<>()
        ).add(value);
    }

    public void cache(SpartanPlayer player, Number number) {
        PatternValue value = new PatternValue(number);
        boolean legitimate = player.getProfile().isLegitimate();

        synchronized (legitimate ? this.available : this.unavailable) {
            synchronized (this.correlations) {
                Map<Integer, Collection<PatternValue>> map =
                        legitimate ? this.available : this.unavailable;
                this.rawCache(map, player, value, 0);

                if (this.generalization) {
                    for (int generalization : this.generalizations) {
                        this.rawCache(
                                map,
                                player,
                                new PatternValue(AlgebraUtils.cut(number.doubleValue(), generalization), value.time),
                                generalization
                        );
                    }
                }
            }
        }
    }

    // Separator

    private void considerLocal(PlayerProfile profile) {
        synchronized (this.blocked) {
            if (this.blocked.contains(profile)) {
                this.blocked.remove(profile);
                Map<Integer, Collection<PatternValue>> map = this.correlations.get(profile);

                if (map != null) {
                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                        synchronized (this.available) {
                            synchronized (this.unavailable) {
                                this.unavailable.get(entry.getKey()).removeAll(entry.getValue());
                                this.available.get(entry.getKey()).addAll(entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    private void blockLocal(PlayerProfile profile) {
        synchronized (this.blocked) {
            if (!this.blocked.contains(profile)) {
                this.blocked.add(profile);
                Map<Integer, Collection<PatternValue>> map = this.correlations.get(profile);

                if (map != null) {
                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                        synchronized (this.available) {
                            synchronized (this.unavailable) {
                                this.available.get(entry.getKey()).removeAll(entry.getValue());
                                this.unavailable.get(entry.getKey()).addAll(entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    // Separator

    public Collection<PatternValue> get() {
        return this.get(0);
    }

    public Collection<PatternValue> get(int generalization) {
        synchronized (this.available) {
            return this.available.get(generalization);
        }
    }

    public Collection<PatternValue> get(SpartanPlayer player, int count) {
        return this.get(player, 0, count);
    }

    public Collection<PatternValue> get(SpartanPlayer player, int generalization, int count) {
        synchronized (this.correlations) {
            Map<Integer, Collection<PatternValue>> map = this.correlations.get(player.uuid);

            if (map != null) {
                Collection<PatternValue> values = map.get(generalization);

                if (values != null) {
                    int size = values.size();

                    if (size > count) {
                        size--;
                        return new ArrayList<>(values).subList(size - count, size);
                    }
                }
            }
            return new ArrayList<>(0);
        }
    }

    // Separator
}
