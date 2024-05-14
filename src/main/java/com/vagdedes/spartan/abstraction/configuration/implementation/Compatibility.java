package com.vagdedes.spartan.abstraction.configuration.implementation;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.*;
import com.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import com.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShotPlus;
import com.vagdedes.spartan.compatibility.manual.building.*;
import com.vagdedes.spartan.compatibility.manual.damage.RealDualWield;
import com.vagdedes.spartan.compatibility.manual.entity.CraftBook;
import com.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import com.vagdedes.spartan.compatibility.manual.packet.ProtocolLib;
import com.vagdedes.spartan.compatibility.manual.vanilla.DragonPhases;
import com.vagdedes.spartan.compatibility.manual.world.AcidRain;
import com.vagdedes.spartan.compatibility.necessary.Floodgate;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import com.vagdedes.spartan.utils.server.ReflectionUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Compatibility {

    private static final String
            fileName = "compatibility",
            staticDirectory = ConfigurationBuilder.getDirectory(fileName);
    private static File file = new File(staticDirectory);
    private static final Map<String, Boolean> bool = new LinkedHashMap<>();

    public enum CompatibilityType {
        ADVANCED_ABILITIES, CRACK_SHOT, CRACK_SHOT_PLUS, CRAFT_BOOK, MAGIC_SPELLS, PROTOCOL_LIB,
        MC_MMO, AUTHENTICATION, TREE_FELLER, VEIN_MINER, GRAPPLING_HOOK, RECENT_PVP_MECHANICS,
        MINE_BOMB, SUPER_PICKAXE, REAL_DUAL_WIELD, MYTHIC_MOBS, ITEM_ATTRIBUTES, PRINTER_MODE,
        VEHICLES, MINE_TINKER, WILD_TOOLS, DRAGON_PHASES, AURELIUM_SKILLS, KNOCKBACK_MASTER,
        MY_PET, CUSTOM_ENCHANTS_PLUS, ECO_ENCHANTS, ITEMS_ADDER, RAMPEN_DRILLS, OLD_COMBAT_MECHANICS,
        CUSTOM_KNOCKBACK, PROJECT_KORRA, ACID_RAIN, FILE_GUI, FLOODGATE, PROTOCOL_SUPPORT;

        private boolean enabled, forced, functional;

        CompatibilityType() {
            this.enabled = false;
            this.forced = false;
            this.functional = false;
        }

        public void refresh(boolean create) {
            boolean hardcoded, contains;
            String name;

            switch (this) {
                case PROTOCOL_SUPPORT: // Necessary
                case FLOODGATE: // Necessary
                case FILE_GUI: // Local
                    hardcoded = true;
                    contains = false;
                    name = this.name().toLowerCase();
                    break;
                case AUTHENTICATION: // General
                    hardcoded = true;
                    contains = true;
                    name = "auth";
                    break;
                default:
                    hardcoded = false;
                    contains = false;
                    name = null;
                    break;
            }

            if (hardcoded) {
                this.enabled = true;
                this.forced = false;
                this.functional = contains ? PluginUtils.contains(name) : PluginUtils.exists(name);
            } else {
                file = new File(staticDirectory);
                String compatibility = this.toString();

                if (create) {
                    ConfigUtils.add(file, compatibility + ".enabled", !this.equals(CompatibilityType.WILD_TOOLS));
                    ConfigUtils.add(file, compatibility + ".force", false);
                }
                this.enabled = getBoolean(compatibility + ".enabled", create);
                this.forced = getBoolean(compatibility + ".force", create);
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isForced() {
            return forced;
        }

        public boolean isFunctional() {
            return enabled && functional;
        }

        // Separator

        public void setFunctional() {
            setFunctional(new String[]{this.toString()}, null, null);
        }

        // Separator

        public void setFunctional(boolean bool) {
            this.functional = bool;
        }

        public void setFunctional(String pluginOrClass) {
            setFunctional(new String[]{pluginOrClass}, null, null);
        }

        public void setFunctional(String[] pluginsOrClasses) {
            setFunctional(pluginsOrClasses, null, null);
        }

        public void setFunctional(CompatibilityType compatibility) {
            setFunctional(null, new CompatibilityType[]{compatibility}, null);
        }

        public void setFunctional(CompatibilityType[] compatibilities) {
            setFunctional(null, compatibilities, null);
        }

        public void setFunctional(Runnable runnable) {
            setFunctional(new String[]{this.toString()}, null, runnable);
        }

        // Separator

        public void setFunctional(String pluginOrClass, Runnable runnable) {
            setFunctional(new String[]{pluginOrClass}, null, runnable);
        }

        public void setFunctional(String[] pluginsOrClasses, Runnable runnable) {
            setFunctional(pluginsOrClasses, null, runnable);
        }

        public void setFunctional(CompatibilityType[] compatibilities, Runnable runnable) {
            setFunctional(null, compatibilities, runnable);
        }

        // Separator

        public void setFunctional(String pluginOrClass, CompatibilityType compatibility) {
            setFunctional(new String[]{pluginOrClass}, new CompatibilityType[]{compatibility}, null);
        }

        public void setFunctional(String[] pluginsOrClasses, CompatibilityType compatibility) {
            setFunctional(pluginsOrClasses, new CompatibilityType[]{compatibility}, null);
        }

        public void setFunctional(String pluginOrClass, CompatibilityType[] compatibilities) {
            setFunctional(new String[]{pluginOrClass}, compatibilities, null);
        }

        public void setFunctional(String[] pluginsOrClasses, CompatibilityType[] compatibilities) {
            setFunctional(pluginsOrClasses, compatibilities, null);
        }

        // Separator

        public void setFunctional(String[] pluginsOrClasses, CompatibilityType[] compatibilities, Runnable runnable) {
            if (this.isEnabled()) {
                boolean function = this.isForced();

                if (!function && pluginsOrClasses != null) {
                    int count = 0, desired = 0;

                    for (String pluginOrClass : pluginsOrClasses) {
                        if (pluginOrClass.length() == 0) {
                            function = true;
                            break;
                        } else {
                            boolean partRequirement = pluginOrClass.endsWith("+");

                            if (partRequirement) {
                                desired++;
                                pluginOrClass = pluginOrClass.substring(0, pluginOrClass.length() - 1);
                            }
                            if (pluginOrClass.contains(".") ? ReflectionUtils.classExists(pluginOrClass) :
                                    pluginOrClass.startsWith("%") ? PluginUtils.contains(pluginOrClass.substring(1)) :
                                            PluginUtils.exists(pluginOrClass)) {
                                if (partRequirement) {
                                    count++;
                                } else {
                                    function = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!function) {
                        function = desired > 0 && count == desired;
                    }
                }

                if (!function && compatibilities != null) {
                    for (CompatibilityType compatibilityType : compatibilities) {
                        if (compatibilityType.isFunctional()) {
                            function = true;
                            break;
                        }
                    }
                }

                if (function) {
                    if (runnable != null) {
                        try {
                            runnable.run();
                        } catch (Exception ex) {
                            this.functional = false;
                            AwarenessNotifications.forcefullySend("Compatibility '" + this + "' failed to load.");
                        }
                    } else {
                        this.functional = true;
                    }
                } else {
                    this.functional = false;
                }
            }
        }
    }

    public File getFile() {
        return file;
    }

    private static void refresh(boolean create) {
        for (CompatibilityType compatibilityType : CompatibilityType.values()) {
            compatibilityType.refresh(create);
        }
        CompatibilityType.MC_MMO.setFunctional();
        CompatibilityType.TREE_FELLER.setFunctional(
                CompatibilityType.TREE_FELLER.toString(),
                CompatibilityType.MC_MMO
        );
        CompatibilityType.CRAFT_BOOK.setFunctional(CraftBook::resetBoatLimit);
        CompatibilityType.CRACK_SHOT.setFunctional(
                () -> Register.enable(new CrackShot(), CrackShot.class)
        );
        CompatibilityType.CRACK_SHOT_PLUS.setFunctional(
                () -> Register.enable(new CrackShotPlus(), CrackShotPlus.class)
        );
        CompatibilityType.CUSTOM_KNOCKBACK.setFunctional("%knockback");
        CompatibilityType.KNOCKBACK_MASTER.setFunctional(
                new String[]{
                        "com.xdefcon.knockbackmaster.api.KnockbackMasterAPI+",
                        CompatibilityType.KNOCKBACK_MASTER + "+"
                }
        );
        CompatibilityType.REAL_DUAL_WIELD.setFunctional(
                () -> Register.enable(new RealDualWield(), RealDualWield.class)
        );
        CompatibilityType.MAGIC_SPELLS.setFunctional(
                () -> Register.enable(new MagicSpells(), MagicSpells.class)
        );
        CompatibilityType.ACID_RAIN.setFunctional(
                new String[]{
                        CompatibilityType.ACID_RAIN.toString(),
                        "acidisland",
                        "askyblock"
                },
                () -> Register.enable(new AcidRain(), AcidRain.class)
        );
        CompatibilityType.ADVANCED_ABILITIES.setFunctional(
                () -> Register.enable(new AdvancedAbilities(), AdvancedAbilities.class)
        );
        CompatibilityType.OLD_COMBAT_MECHANICS.setFunctional();
        CompatibilityType.DRAGON_PHASES.setFunctional(
                new String[]{
                        "org.bukkit.event.entity.EnderDragonChangePhaseEvent"
                },
                () -> Register.enable(new DragonPhases(), DragonPhases.class)
        );
        CompatibilityType.VEIN_MINER.setFunctional(VeinMiner::reload);
        CompatibilityType.PROJECT_KORRA.setFunctional(
                () -> Register.enable(new ProjectKorra(), ProjectKorra.class)
        );
        CompatibilityType.GRAPPLING_HOOK.setFunctional(
                () -> Register.enable(new GrapplingHook(), GrapplingHook.class)
        );
        CompatibilityType.MYTHIC_MOBS.setFunctional(MythicMobs::reload);
        CompatibilityType.CUSTOM_ENCHANTS_PLUS.setFunctional();
        CompatibilityType.ECO_ENCHANTS.setFunctional(ReflectionUtils.classExists("com.willfp.ecoenchants.enchants.EcoEnchant"));
        CompatibilityType.VEHICLES.setFunctional(
                () -> Register.enable(new Vehicles(), Vehicles.class)
        );
        CompatibilityType.MINE_TINKER.setFunctional(
                () -> Register.enable(new MineTinker(), MineTinker.class)
        );
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            CompatibilityType.ITEM_ATTRIBUTES.setFunctional(
                    new String[]{""},
                    new CompatibilityType[]{
                            CompatibilityType.MINE_TINKER,
                            CompatibilityType.MYTHIC_MOBS,
                            CompatibilityType.PROJECT_KORRA
                    }
            );
            CompatibilityType.RECENT_PVP_MECHANICS.setFunctional(
                    new String[]{""}
            );
        }
        CompatibilityType.WILD_TOOLS.setFunctional(
                () -> Register.enable(new WildTools(), WildTools.class)
        );
        CompatibilityType.FLOODGATE.setFunctional(
                new String[]{
                        "%" + CompatibilityType.FLOODGATE,
                        "%geyser"
                },
                Floodgate::reload
        );
        CompatibilityType.PROTOCOL_SUPPORT.setFunctional(
                new String[]{
                        "protocolsupport.api.Connection+",
                        "protocolsupport.api.ProtocolSupportAPI+",
                        CompatibilityType.PROTOCOL_SUPPORT + "+"
                }
        );
        CompatibilityType.PROTOCOL_LIB.setFunctional(ProtocolLib::reload);
        CompatibilityType.MY_PET.setFunctional();
        CompatibilityType.RAMPEN_DRILLS.setFunctional(
                () -> Register.enable(new RampenDrills(), RampenDrills.class)
        );
        CompatibilityType.MINE_BOMB.setFunctional(MineBomb::reload);
        CompatibilityType.PRINTER_MODE.setFunctional();
        CompatibilityType.SUPER_PICKAXE.setFunctional(
                new String[]{
                        CompatibilityType.SUPER_PICKAXE.toString(),
                        CompatibilityType.SUPER_PICKAXE + "Reloaded"
                }
        );
        CompatibilityType.AURELIUM_SKILLS.setFunctional(
                () -> Register.enable(new AureliumSkills(), AureliumSkills.class)
        );
        CompatibilityType.ITEMS_ADDER.setFunctional();
        MainMenu.refresh();
    }

    private static boolean getBoolean(String path, boolean create) {
        Boolean data = bool.get(path);

        if (data != null) {
            return data;
        }
        if (!file.exists()) {
            if (!create) {
                return false;
            }
            create(false);
        }
        boolean value = YamlConfiguration.loadConfiguration(file).getBoolean(path);
        bool.put(path, value);
        return value;
    }

    public void clear() {
        bool.clear();
        fastClear();
    }

    public void fastClear() {
        refresh(false);
    }

    public static void create(boolean local) {
        file = new File(staticDirectory);
        boolean exists = file.exists();
        bool.clear();
        refresh(true);

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    // Separator

    public List<CompatibilityType> getActiveCompatibilities() {
        CompatibilityType[] compatibilities = CompatibilityType.values();
        List<CompatibilityType> active = new ArrayList<>(compatibilities.length);

        for (CompatibilityType compatibility : compatibilities) {
            if (compatibility.isFunctional()) {
                active.add(compatibility);
            }
        }
        return active;
    }

    public List<CompatibilityType> getTotalCompatibilities() {
        CompatibilityType[] compatibilities = CompatibilityType.values();
        List<CompatibilityType> active = new ArrayList<>(compatibilities.length);

        for (CompatibilityType compatibility : CompatibilityType.values()) {
            if (compatibility.isEnabled()) {
                active.add(compatibility);
            }
        }
        return active;
    }

    public void evadeFalsePositives(SpartanPlayer player,
                                    Compatibility.CompatibilityType compatibilityType,
                                    Enums.HackType[] hackTypes,
                                    int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
        }
    }

    public void evadeFalsePositives(SpartanPlayer player,
                                    Compatibility.CompatibilityType compatibilityType,
                                    Enums.HackType hackType,
                                    int ticks) {
        player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
    }

    public void evadeFalsePositives(SpartanPlayer player,
                                    Compatibility.CompatibilityType compatibilityType,
                                    Enums.HackCategoryType[] types,
                                    int ticks) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            for (Enums.HackCategoryType type : types) {
                if (hackType.category == type) {
                    player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
                    break;
                }
            }
        }
    }

    public void evadeFalsePositives(SpartanPlayer player,
                                    Compatibility.CompatibilityType compatibilityType,
                                    Enums.HackCategoryType type,
                                    int ticks) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (hackType.category == type) {
                player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
            }
        }
    }
}
