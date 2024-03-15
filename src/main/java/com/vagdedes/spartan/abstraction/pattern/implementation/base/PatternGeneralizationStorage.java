package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

public class PatternGeneralizationStorage extends PatternGeneralization {

    private short count;
    private File file;
    private YamlConfiguration configuration;
    private byte store;

    PatternGeneralizationStorage(String key) {
        super(key, 0);
        this.count = 0;
        this.store = 0;
        this.reload();
    }

    private void reload() {
        int hash = Objects.hash(this.key, this.generalization, System.currentTimeMillis());
        this.file = new File(path(key) + "/" + hash + ".yml");

        if (this.file.exists()) {
            this.reload();
            return;
        } else {
            try {
                this.file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    void set(PlayerProfile profile, PatternValue value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(value.time + profileOption, profile.getName());
            this.configuration.set(value.time + situationOption, value.situation);
            this.configuration.set(value.time + patternOption, value.pattern);

            if (this.store == 0) {
                this.store = 1;
            }
        });
    }

    void store() {
        if (this.store == 1) {
            this.store = 2;

            SpartanBukkit.dataThread.execute(() -> {
                try {
                    this.configuration.save(this.file);
                    this.store = 0;

                    if (this.count < 0) { // Short has overflowed (32767)
                        this.count = 0;
                        this.reload();
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }
}
