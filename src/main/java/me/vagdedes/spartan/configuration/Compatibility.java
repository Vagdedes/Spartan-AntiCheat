package me.vagdedes.spartan.configuration;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.abstraction.ConfigurationBuilder;
import me.vagdedes.spartan.checks.movement.speed.Speed;
import me.vagdedes.spartan.compatibility.manual.abilities.*;
import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShotPlus;
import me.vagdedes.spartan.compatibility.manual.building.*;
import me.vagdedes.spartan.compatibility.manual.damage.RealDualWield;
import me.vagdedes.spartan.compatibility.manual.damage.SmashHit;
import me.vagdedes.spartan.compatibility.manual.entity.CraftBook;
import me.vagdedes.spartan.compatibility.manual.entity.Vehicles;
import me.vagdedes.spartan.compatibility.manual.essential.Essentials;
import me.vagdedes.spartan.compatibility.manual.essential.MinigameMaker;
import me.vagdedes.spartan.compatibility.manual.essential.protocollib.ProtocolLib;
import me.vagdedes.spartan.compatibility.manual.vanilla.DragonPhases;
import me.vagdedes.spartan.compatibility.manual.world.AcidRain;
import me.vagdedes.spartan.compatibility.necessary.bedrock.plugins.Floodgate;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.gui.spartan.MainMenu;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.utils.server.ReflectionUtils;
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
        AdvancedAbilities, CrackShot, CrackShotPlus, CraftBook, Essentials, MagicSpells, ProtocolLib, mcMMO, NoHitDelay,
        TreeFeller, VeinMiner, GrapplingHook, ViaRewind, RecentPvPMechanics, MineBomb, SmashHit, SuperPickaxe,
        RealDualWield, UltimateStatistics, MythicMobs, ViaVersion, ItemAttributes, PrinterMode, Vehicles, MineTinker,
        MinigameMaker, WildTools, DragonPhases, AureliumSkills, KnockbackMaster, MyPet, CustomEnchantsPlus,
        EcoEnchants, ItemsAdder, RampenDrills, OldCombatMechanics, CustomKnockback, ProjectKorra, AcidRain,
        MajorIncompatibility, FileGUI, WorldGuard, Floodgate, QuickShop, AntiAltAccount, ProtocolSupport, Authentication;

        private boolean enabled, forced, functional;

        CompatibilityType() {
            this.enabled = false;
            this.forced = false;
            this.functional = false;
        }

        public void refresh(boolean create) {
            boolean hardcoded;

            switch (this) {
                case QuickShop: // Specific
                case ProtocolSupport:
                case WorldGuard:
                case Floodgate:
                case FileGUI: // Local
                case AntiAltAccount:
                case Authentication: // General
                    hardcoded = true;
                    break;
                default:
                    hardcoded = false;
                    break;
            }

            if (hardcoded) {
                this.enabled = true;
                this.forced = false;
                this.functional = PluginUtils.exists(this.name().toLowerCase());
            } else {
                file = new File(staticDirectory);
                String compatibility = this.toString();

                if (create) {
                    ConfigUtils.add(file, compatibility + ".enabled", !this.equals(CompatibilityType.WildTools));
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
            return enabled && functional && !CloudFeature.hasException();
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
        CompatibilityType.mcMMO.setFunctional();
        CompatibilityType.TreeFeller.setFunctional(
                CompatibilityType.TreeFeller.toString(),
                CompatibilityType.mcMMO
        );
        CompatibilityType.CraftBook.setFunctional(CraftBook::resetBoatLimit);
        CompatibilityType.CrackShot.setFunctional(
                () -> Register.enable(new CrackShot(), CrackShot.class)
        );
        CompatibilityType.CrackShotPlus.setFunctional(
                () -> Register.enable(new CrackShotPlus(), CrackShotPlus.class)
        );
        CompatibilityType.CustomKnockback.setFunctional("%knockback");
        CompatibilityType.KnockbackMaster.setFunctional(
                new String[]{
                        "com.xdefcon.knockbackmaster.api.KnockbackMasterAPI+",
                        CompatibilityType.KnockbackMaster + "+"
                }
        );
        CompatibilityType.RealDualWield.setFunctional(
                () -> Register.enable(new RealDualWield(), RealDualWield.class)
        );
        CompatibilityType.MagicSpells.setFunctional(
                () -> Register.enable(new MagicSpells(), MagicSpells.class)
        );
        CompatibilityType.MajorIncompatibility.setFunctional(
                new String[]{
                        "Skript",
                        "CMI",
                        "SlimeFun",
                        "EcoSkills"
                }
        );
        CompatibilityType.AcidRain.setFunctional(
                new String[]{
                        CompatibilityType.AcidRain.toString(),
                        "acidisland",
                        "askyblock"
                },
                () -> Register.enable(new AcidRain(), AcidRain.class)
        );
        CompatibilityType.AdvancedAbilities.setFunctional(
                () -> Register.enable(new AdvancedAbilities(), AdvancedAbilities.class)
        );
        CompatibilityType.OldCombatMechanics.setFunctional();
        CompatibilityType.DragonPhases.setFunctional(
                new String[]{
                        "org.bukkit.event.entity.EnderDragonChangePhaseEvent"
                },
                () -> Register.enable(new DragonPhases(), DragonPhases.class)
        );
        CompatibilityType.VeinMiner.setFunctional(VeinMiner::reload);
        CompatibilityType.ViaVersion.setFunctional();
        CompatibilityType.ProjectKorra.setFunctional(
                () -> Register.enable(new ProjectKorra(), ProjectKorra.class)
        );
        CompatibilityType.GrapplingHook.setFunctional(
                () -> Register.enable(new GrapplingHook(), GrapplingHook.class)
        );
        CompatibilityType.UltimateStatistics.setFunctional();
        CompatibilityType.MinigameMaker.setFunctional(MinigameMaker::reload);
        CompatibilityType.MythicMobs.setFunctional(MythicMobs::reload);
        CompatibilityType.CustomEnchantsPlus.setFunctional();
        CompatibilityType.EcoEnchants.setFunctional();
        CompatibilityType.Vehicles.setFunctional(
                () -> Register.enable(new Vehicles(), Vehicles.class)
        );
        CompatibilityType.MineTinker.setFunctional(
                () -> Register.enable(new MineTinker(), MineTinker.class)
        );
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            CompatibilityType.ItemAttributes.setFunctional(
                    new String[]{""},
                    new CompatibilityType[]{
                            CompatibilityType.MineTinker,
                            CompatibilityType.MythicMobs,
                            CompatibilityType.ProjectKorra,
                            CompatibilityType.MajorIncompatibility
                    }
            );
            CompatibilityType.RecentPvPMechanics.setFunctional(
                    new String[]{""}
            );
        }
        CompatibilityType.WildTools.setFunctional(
                () -> Register.enable(new WildTools(), WildTools.class)
        );
        CompatibilityType.Floodgate.setFunctional(
                new String[]{
                        "%" + CompatibilityType.Floodgate,
                        "%geyser"
                },
                Floodgate::reload
        );
        CompatibilityType.ProtocolSupport.setFunctional(
                new String[]{
                        "protocolsupport.api.Connection+",
                        "protocolsupport.api.ProtocolSupportAPI+",
                        CompatibilityType.ProtocolSupport + "+"
                }
        );
        CompatibilityType.ProtocolLib.setFunctional(ProtocolLib::reload);
        CompatibilityType.MyPet.setFunctional();
        CompatibilityType.RampenDrills.setFunctional(
                () -> Register.enable(new RampenDrills(), RampenDrills.class)
        );
        CompatibilityType.MineBomb.setFunctional(MineBomb::reload);
        CompatibilityType.PrinterMode.setFunctional();
        CompatibilityType.SuperPickaxe.setFunctional(
                new String[]{
                        CompatibilityType.SuperPickaxe.toString(),
                        CompatibilityType.SuperPickaxe + "Reloaded"
                }
        );
        CompatibilityType.ViaRewind.setFunctional(Speed::updateLimits);
        CompatibilityType.AureliumSkills.setFunctional(
                () -> Register.enable(new AureliumSkills(), AureliumSkills.class)
        );
        CompatibilityType.SmashHit.setFunctional(
                () -> Register.enable(new SmashHit(), SmashHit.class)
        );
        CompatibilityType.ItemsAdder.setFunctional();
        CompatibilityType.Essentials.setFunctional(Essentials::reload);
        CompatibilityType.NoHitDelay.setFunctional(
                new String[]{""}
        );
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

    static void create(boolean local) {
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

    public List<CompatibilityType> getInactiveCompatibilities() {
        CompatibilityType[] compatibilities = CompatibilityType.values();
        List<CompatibilityType> active = new ArrayList<>(compatibilities.length);

        for (CompatibilityType compatibility : CompatibilityType.values()) {
            if (!compatibility.isFunctional()) {
                active.add(compatibility);
            }
        }
        return active;
    }
}
