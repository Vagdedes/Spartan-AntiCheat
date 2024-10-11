package me.vagdedes.spartan.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Material;

public class Enums {

    // API Use
    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.XRay.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to see through blocks",
                        "in order to find rare ores, such as diamonds,",
                        "gold, and even emerald. (Logs must be enabled)"
                }
        ),
        Exploits(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.exploits.Exploits.class,
                new String[]{
                        "This check will prevent client",
                        "modules that may potentially hurt",
                        "a server's functional performance."
                }
        ),
        NoSwing(
                HackCategoryType.PLAYER,
                com.vagdedes.spartan.abstraction.check.implementation.player.NoSwing.class,
                new String[]{
                        "This check will prevent client modules",
                        "that manipulate packets and prevent",
                        "interaction animations from being shown."
                }
        ),
        IrregularMovements(
                HackCategoryType.MOVEMENT,
                com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to move abnormally,",
                        "such as stepping blocks or climbing walls."
                }
        ),
        ImpossibleActions(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions.ImpossibleActions.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to execute actions",
                        "in abnormal cases, such as when sleeping."
                }
        ),
        ItemDrops(
                HackCategoryType.INVENTORY,
                com.vagdedes.spartan.abstraction.check.implementation.inventory.ItemDrops.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to drop an amount",
                        "of items in abnormally fast rates."
                }
        ),
        AutoRespawn(
                HackCategoryType.PLAYER,
                com.vagdedes.spartan.abstraction.check.implementation.player.AutoRespawn.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to respawn faster",
                        "than what is physically expected."
                }
        ),
        InventoryClicks(
                HackCategoryType.INVENTORY,
                com.vagdedes.spartan.abstraction.check.implementation.inventory.InventoryClicks.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with an",
                        "amount of items, in abnormally fast rates."
                }
        ),
        Criticals(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.Criticals.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to critical damage",
                        "an entity without properly moving."
                }
        ),
        GhostHand(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.GhostHand.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact or break",
                        "blocks through walls of blocks."
                }
        ),
        BlockReach(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.BlockReach.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to build or break",
                        "blocks within an abnormally long distance."
                }
        ),
        FastBow(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.FastBow.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to shoot arrows",
                        "in abnormally fast rates."
                }
        ),
        FastClicks(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.FastClicks.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to click abnormally fast",
                        "or have an irregular clicking consistency."
                }
        ),
        FastHeal(
                HackCategoryType.PLAYER,
                com.vagdedes.spartan.abstraction.check.implementation.player.FastHeal.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to heal faster",
                        "than what is physically allowed."
                }
        ),
        ImpossibleInventory(
                HackCategoryType.INVENTORY,
                com.vagdedes.spartan.abstraction.check.implementation.inventory.ImpossibleInventory.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with",
                        "an inventory in abnormal cases, such",
                        "as when sprinting or walking."
                }
        ),
        HitReach(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.HitReach.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to hit entities",
                        "from an abnormally long distance"
                }
        ),
        FastBreak(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.FastBreak.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to break one or multiple",
                        "blocks irregularly fast."
                }
        ),
        Speed(
                HackCategoryType.MOVEMENT,
                com.vagdedes.spartan.abstraction.check.implementation.movement.Speed.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to travel faster",
                        "than what is physically allowed."
                }
        ),
        FastPlace(
                HackCategoryType.WORLD,
                com.vagdedes.spartan.abstraction.check.implementation.world.FastPlace.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to place blocks",
                        "in abnormally fast rates."
                }
        ),
        MorePackets(
                HackCategoryType.MOVEMENT,
                com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets.MorePackets.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to send abnormally",
                        "high amounts of movement packets."
                }
        ),
        Simulation(
                HackCategoryType.MOVEMENT,
                com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.Simulation.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to take advantage",
                        "of any type of movement behavior."
                }
        ),
        FastEat(
                HackCategoryType.PLAYER,
                com.vagdedes.spartan.abstraction.check.implementation.player.FastEat.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to consume an amount",
                        "of food in an abnormal amount of time."
                }
        ),
        Velocity(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.Velocity.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to receive abnormal",
                        "amounts of knockback, or none at all."
                }
        ),
        KillAura(
                HackCategoryType.COMBAT,
                com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.KillAura.class,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to have an 'apparent'",
                        "combat advantage against any entity."
                }
        );

        private Check check;
        public final HackCategoryType category;
        public final Class<?> executor;
        public final String[] description;

        HackType(HackCategoryType category, Class<?> executor, String[] description) {
            this.category = category;
            this.executor = executor;
            this.description = description;
            this.check = new Check(this);
        }

        public Check getCheck() {
            return check;
        }

        public void resetCheck() {
            if (this.check != null) {
                this.check = new Check(this);
            }
        }
    }

    // API Use
    public enum Permission {
        CONDITION, WAVE, RECONNECT, ADMIN, RELOAD,
        KICK, BYPASS, MANAGE, INFO, CHAT_PROTECTION, WARN, USE_BYPASS,
        NOTIFICATIONS, BEDROCK;

        private final String key;

        Permission() {
            key = Register.plugin.getName().toLowerCase() + "." + this.name().toLowerCase();
        }

        public String getKey() {
            return key;
        }
    }

    // API Use
    public enum ToggleAction {
        ENABLE, DISABLE
    }

    public enum HackCategoryType {
        COMBAT(Material.IRON_SWORD),
        MOVEMENT(Material.FEATHER),
        PLAYER(Material.STICK),
        WORLD(Material.DIAMOND_PICKAXE),
        INVENTORY(MaterialUtils.get("crafting_table"));

        private final String string;
        public final Material material;

        HackCategoryType(Material material) {
            this.material = material;

            switch (this.ordinal()) {
                case 0:
                    string = "Combat";
                    break;
                case 1:
                    string = "Movement";
                    break;
                case 2:
                    string = "Player";
                    break;
                case 3:
                    string = "World";
                    break;
                default:
                    string = "Inventory";
                    break;
            }
        }

        @Override
        public String toString() {
            return string;
        }

    }

}
