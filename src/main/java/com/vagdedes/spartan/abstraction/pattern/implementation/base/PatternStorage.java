package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.probability.ProbabilityRank;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternStorage {

    private static final boolean debug = true;
    private static final Collection<PatternStorage> instances = Collections.synchronizedList(new ArrayList<>());
    private static final Map<Integer, PatternAllCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, PatternSpecificCache> specificCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, PatternRankedCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                synchronized (instance.correlations) {
                    if (instance.correlations.remove(profile) != null) {
                        File directory = new File(PatternGeneralization.path(instance.key));

                        if (directory.isDirectory()) {
                            File[] files = directory.listFiles();

                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile()
                                            && file.getName().endsWith(".yml")) {
                                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                                        Set<String> keys = configuration.getKeys(false);

                                        if (!keys.isEmpty()) {
                                            boolean found = false;

                                            for (String key : keys) {
                                                try {
                                                    String profileName = configuration.getString(key + PatternGeneralization.profileOption, null);

                                                    if (profileName != null
                                                            && profileName.equals(profile.getName())) {
                                                        for (String option : PatternGeneralization.options) {
                                                            configuration.set(key + option, null);
                                                        }
                                                        configuration.set(key, null);
                                                        found = true;
                                                    }
                                                } catch (Exception ignored) {
                                                }
                                            }

                                            if (found) {
                                                try {
                                                    configuration.save(file);
                                                } catch (Exception ignored) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void reload() {
        synchronized (instances) {
            for (PatternStorage patternStorage : instances) {
                patternStorage.load();
            }
        }
    }

    public static void clear() {
        synchronized (instances) {
            synchronized (allCache) {
                synchronized (specificCache) {
                    synchronized (rankedCache) {
                        instances.clear();
                        allCache.clear();
                        specificCache.clear();
                        rankedCache.clear();
                    }
                }
            }
        }
    }

    // Separator

    public final String key;
    private final PatternGeneralization[] generalizations;
    // Profile -> Generalization -> Situation -> Patterns
    private final Map<PlayerProfile, Map<Integer, Map<Long, List<PatternValue>>>> correlations;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());

        this.generalizations = new PatternGeneralization[generalizations.length + 1];
        this.generalizations[0] = new PatternGeneralizationStorage(key);

        if (generalizations.length > 0) {
            for (int count = 0; count < generalizations.length; count++) {
                this.generalizations[count + 1] = new PatternGeneralizationMemory(key, generalizations[count]);
            }
        }

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        ((PatternGeneralizationStorage) this.generalizations[0]).store();
    }

    private void load() {
        store();
        Map<PlayerProfile, Map<Integer, Map<Long, List<PatternValue>>>> correlations = new LinkedHashMap<>();
        File directory = new File(PatternGeneralization.path(this.key));

        if (!directory.exists()) {
            directory.mkdirs();
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file.isFile()
                            && file.getName().endsWith(".yml")) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                        Set<String> keys = configuration.getKeys(false);

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                try {
                                    long time = Long.parseLong(key);
                                    Object pattern = configuration.get(key + PatternGeneralization.patternOption, null);

                                    if (pattern instanceof Number) {
                                        long situation = configuration.getLong(key + PatternGeneralization.situationOption, Long.MIN_VALUE);

                                        if (situation != Long.MIN_VALUE) {
                                            String profileName = configuration.getString(key + PatternGeneralization.profileOption, null);

                                            if (profileName != null
                                                    && !profileName.isEmpty()) {
                                                PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);

                                                for (PatternGeneralization generalization : this.generalizations) {
                                                    this.rawCache(
                                                            correlations,
                                                            profile,
                                                            new PatternValue(
                                                                    situation,
                                                                    (Number) pattern,
                                                                    generalization,
                                                                    time
                                                            ),
                                                            generalization,
                                                            false
                                                    );
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            } else {
                directory.delete();
                directory.mkdirs();
            }
        }

        synchronized (this.correlations) {
            this.correlations.clear();
            this.correlations.putAll(correlations);
        }
    }

    // Separator

    private void rawCache(Map<PlayerProfile, Map<Integer, Map<Long, List<PatternValue>>>> correlations,
                          PlayerProfile profile,
                          PatternValue value,
                          PatternGeneralization generalization,
                          boolean set) {
        List<PatternValue> list = correlations.computeIfAbsent(
                profile,
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                generalization.generalization,
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                value.situation,
                k -> new ArrayList<>(100)
        );
        list.add(value);

        if (list.size() > (SpartanEdition.is2ndGeneration() ? 3_000_000 : 1_000_000)) {
            list.remove(0);
        }

        if (set) {
            ((PatternGeneralizationStorage) generalization).set(profile, value);
        }
    }

    public void collect(PlayerProfile profile, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();

            synchronized (this.correlations) {
                for (PatternGeneralization generalization : this.generalizations) {
                    this.rawCache(
                            this.correlations,
                            profile,
                            new PatternValue(
                                    situation,
                                    number,
                                    generalization,
                                    time
                            ),
                            generalization,
                            generalization instanceof PatternGeneralizationStorage
                    );
                }
            }
        }
    }

    // Separator

    private void notify(SpartanPlayer player, long situation) {
        String message = AwarenessNotifications.getOptionalNotification(
                "Spartan's Machine Learning algorithms have insufficient data to check you. "
                        + (player.getProfile().isLegitimate()
                        ? "Continue playing LEGITIMATELY to train the algorithm so you can try again later."
                        : "Since you are " + player.getProfile().evidence.getType() + ", either clear your data via '/spartan info'"
                        + " and play LEGITIMATELY or find a legitimate player to help train the algorithm so you can try again later.")
        );

        if (message != null) {
            String s = "pattern-storage-" + key;

            if (AwarenessNotifications.canSend(player.uuid, s + "-" + situation, 120)
                    && AwarenessNotifications.canSend(player.uuid, s, 60)) {
                player.sendMessage(message);
            }
        }
    }

    // Separator

    public Collection<Collection<PatternValue>> getAll(SpartanPlayer player, long situation, int generalization) {
        int hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplier) + generalization;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<PatternValue>> result = this.getAllRaw(situation, generalization);

            synchronized (allCache) {
                PatternAllCache cache = allCache.get(hash);

                if (result.isEmpty()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (allCache) {
                PatternAllCache cache = allCache.get(hash);

                if (cache == null) {
                    Collection<Collection<PatternValue>> result = this.getAllRaw(situation, generalization);
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getAllRaw(situation, generalization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<PatternValue>> getAllRaw(long situation, int generalization) {
        if (this.correlations.isEmpty()) {
            return new ArrayList<>(0);
        } else {
            Collection<Collection<PatternValue>> collections = new ArrayList<>(this.correlations.size());
            int count = 0;

            synchronized (this.correlations) {
                for (Map.Entry<PlayerProfile, Map<Integer, Map<Long, List<PatternValue>>>> entry : this.correlations.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        Collection<PatternValue> data = entry.getValue().get(generalization).get(situation);

                        if (data != null) {
                            collections.add(data);
                            count += data.size();
                        }
                    }
                }
            }
            return !debug && (collections.size() < ResearchEngine.profileRequirement
                    || count < ResearchEngine.dataRequirement)
                    ? new ArrayList<>(0)
                    : collections;
        }
    }

    // Separator

    public List<PatternValue> getSpecific(PlayerProfile profile, long situation, int generalization, int count) {
        int hash = (profile.hashCode() * SpartanBukkit.hashCodeMultiplier) + Long.hashCode(situation);
        hash = (hash * SpartanBukkit.hashCodeMultiplier) + generalization;

        synchronized (specificCache) {
            PatternSpecificCache cache = specificCache.get(hash);
            boolean notNull = cache != null;

            if (notNull && !cache.isExpired()) {
                return cache.data;
            } else {
                synchronized (this.correlations) {
                    Map<Integer, Map<Long, List<PatternValue>>> map = this.correlations.get(profile);
                    List<PatternValue> list;

                    if (map == null) {
                        list = new ArrayList<>(0);
                    } else {
                        List<PatternValue> specific = map.get(generalization).get(situation);

                        if (specific == null) {
                            list = new ArrayList<>(0);
                        } else {
                            list = this.subList(specific, count);
                        }
                    }
                    if (notNull) {
                        cache.update(list);
                    } else {
                        specificCache.put(
                                hash,
                                new PatternSpecificCache(list)
                        );
                    }
                    return list;
                }
            }
        }
    }

    public List<PatternValue> subList(List<PatternValue> specific, int count) {
        int size = specific.size();

        if (size < count) {
            return new ArrayList<>(0);
        } else {
            return specific.subList(size - count, size);
        }
    }

    // Separator

    public ProbabilityRank getRanked(SpartanPlayer player, long situation, int generalization) {
        int hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplier) + generalization;

        if (DetectionNotifications.isEnabled(player)) {
            ProbabilityRank result = this.getRankedRaw(situation, generalization);

            synchronized (rankedCache) {
                PatternRankedCache cache = rankedCache.get(hash);

                if (!result.hasData()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    rankedCache.put(hash, new PatternRankedCache(result));
                    return result;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (rankedCache) {
                PatternRankedCache cache = rankedCache.get(hash);

                if (cache == null) {
                    ProbabilityRank result = this.getRankedRaw(situation, generalization);
                    rankedCache.put(
                            hash,
                            new PatternRankedCache(result)
                    );
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getRankedRaw(situation, generalization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    public ProbabilityRank getRankedRaw(long situation, int generalization) {
        if (this.correlations.isEmpty()) {
            return new ProbabilityRank(0);
        } else {
            ProbabilityRank rank = new ProbabilityRank(this.correlations.size());
            int profiles = 0;

            synchronized (this.correlations) {
                for (Map.Entry<PlayerProfile, Map<Integer, Map<Long, List<PatternValue>>>> entry : this.correlations.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        Collection<PatternValue> data = entry.getValue().get(generalization).get(situation);

                        if (data != null) {
                            rank.addMultiplePatterns(data);
                            profiles++;
                        }
                    }
                }
            }
            return !debug
                    && (profiles < ResearchEngine.profileRequirement
                    || rank.getTotal() < ResearchEngine.dataRequirement)
                    ? new ProbabilityRank(0)
                    : rank;
        }
    }
}
