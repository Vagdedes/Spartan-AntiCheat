package me.vagdedes.spartan.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;

public class Enums {

    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to see through blocks",
                        "in order to find rare ores, such as diamonds,",
                        "gold, and even emerald. (Logs must be enabled)"
                }
        ),
        Exploits(
                HackCategoryType.EXPLOITS,
                new String[]{
                        "This check will prevent client",
                        "modules that may potentially hurt",
                        "a server's functional performance."
                }
        ),
        NoSwing(
                HackCategoryType.PLAYER,
                new String[]{
                        "This check will prevent client modules",
                        "that manipulate packets and prevent",
                        "interaction animations from being shown."
                }
        ),
        IrregularMovements(
                HackCategoryType.MOVEMENT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to move abnormally,",
                        "such as stepping blocks or climbing walls."
                }
        ),
        ImpossibleActions(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to execute actions",
                        "in abnormal cases, such as when sleeping."
                }
        ),
        ItemDrops(
                HackCategoryType.INVENTORY,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to drop an amount",
                        "of items in abnormally fast rates."
                }
        ),
        AutoRespawn(
                HackCategoryType.PLAYER,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to respawn faster",
                        "than what is physically expected."
                }
        ),
        InventoryClicks(
                HackCategoryType.INVENTORY,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with an",
                        "amount of items, in abnormally fast rates."
                }
        ),
        Criticals(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to critical damage",
                        "an entity without properly moving."
                }
        ),
        GhostHand(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact or break",
                        "blocks through walls of blocks."
                }
        ),
        BlockReach(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to build or break",
                        "blocks within an abnormally long distance."
                }
        ),
        FastBow(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to shoot arrows",
                        "in abnormally fast rates."
                }
        ),
        FastClicks(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to click abnormally fast",
                        "or have an irregular clicking consistency."
                }
        ),
        FastHeal(
                HackCategoryType.PLAYER,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to heal faster",
                        "than what is physically allowed."
                }
        ),
        ImpossibleInventory(
                HackCategoryType.INVENTORY,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to interact with",
                        "an inventory in abnormal cases, such",
                        "as when sprinting or walking."
                }
        ),
        HitReach(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to hit entities",
                        "from an abnormally long distance"
                }
        ),
        FastBreak(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to break one or multiple",
                        "blocks irregularly fast."
                }
        ),
        Speed(
                HackCategoryType.MOVEMENT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to travel faster",
                        "than what is physically allowed."
                }
        ),
        FastPlace(
                HackCategoryType.WORLD,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to place blocks",
                        "in abnormally fast rates."
                }
        ),
        MorePackets(
                HackCategoryType.MOVEMENT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to send abnormally",
                        "high amounts of movement packets."
                }
        ),
        NoFall(
                HackCategoryType.MOVEMENT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to decrease or",
                        "eliminate falling damage."
                }
        ),
        FastEat(
                HackCategoryType.PLAYER,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to consume an amount",
                        "of food in an abnormal amount of time."
                }
        ),
        Velocity(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to receive abnormal",
                        "amounts of knockback, or none at all."
                }
        ),
        KillAura(
                HackCategoryType.COMBAT,
                new String[]{
                        "This check will prevent client modules",
                        "that allow a player to have an 'apparent'",
                        "combat advantage against any entity."
                }
        );

        private Check check;
        public final HackCategoryType category;
        public final String[] description;

        HackType(HackCategoryType category, String[] description) {
            this.check = null;
            this.category = category;
            this.description = description;
        }

        public Check getCheck() {
            return check != null ? check : (check = new Check(this));
        }

        public void resetCheck() {
            if (this.check != null) {
                this.check.clearConfigurationCache();
                this.check = new Check(
                        this,
                        this.check.copyIgnoredViolations(),
                        true
                );
            }
        }
    }

    public enum MiningOre {
        ANCIENT_DEBRIS, DIAMOND, EMERALD, GOLD;

        private final String string;

        MiningOre() {
            string = this.name().toLowerCase().replace("_", "-");
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public enum Permission {
        CONDITION, STAFF_CHAT, WAVE, RECONNECT, ADMIN, RELOAD,
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

    public enum ToggleAction {
        ENABLE, DISABLE
    }

    public enum HackCategoryType {
        COMBAT, MOVEMENT, EXPLOITS, PLAYER, WORLD, INVENTORY;

        private final String string;

        HackCategoryType() {
            switch (this.ordinal()) {
                case 0:
                    string = "Combat";
                    break;
                case 1:
                    string = "Movement";
                    break;
                case 2:
                    string = "Exploits";
                    break;
                case 3:
                    string = "Player";
                    break;
                case 4:
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

    public enum DataType {
        JAVA, BEDROCK, UNIVERSAL;

        public final String lowerCase, name;

        DataType() {
            switch (this.ordinal()) {
                case 0:
                    lowerCase = "java";
                    name = "Java";
                    break;
                case 1:
                    lowerCase = "bedrock";
                    name = "Bedrock";
                    break;
                default:
                    lowerCase = "universal";
                    name = "Universal";
                    break;
            }
        }
    }
}
