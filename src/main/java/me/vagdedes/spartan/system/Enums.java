package me.vagdedes.spartan.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;

public class Enums {

    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay, Exploits, NoSwing, IrregularMovements, ImpossibleActions, ItemDrops, AutoRespawn, InventoryClicks,
        NoSlowdown, Criticals, GhostHand, BlockReach, FastBow, FastClicks, FastHeal, ImpossibleInventory, HitReach, FastBreak,
        Speed, FastPlace, MorePackets, NoFall, FastEat, Velocity, KillAura;

        private Check check;

        HackType() {
            this.check = null;
        }

        public Check getCheck() {
            return check != null ? check : (check = new Check(this));
        }

        public void resetCheck() {
            if (this.check != null) {
                this.check.clearConfigurationCache();
                this.check = new Check(
                        this,
                        this.check.copyMaxCancelledViolations(),
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
        NOTIFICATIONS;

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

    public enum Debug {
        COMBAT, MOVEMENT, MISC;

        private final String string;

        Debug() {
            switch (this.ordinal()) {
                case 0:
                    string = "Combat";
                    break;
                case 1:
                    string = "Movement";
                    break;
                default:
                    string = "Misc";
                    break;
            }
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public enum CheckType {
        COMBAT, MOVEMENT, EXPLOITS, PLAYER, WORLD, INVENTORY;

        private final String string;

        CheckType() {
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

    public enum ArmorState {
        Full, Semi, Empty
    }

    public enum DataType {
        Java, Bedrock, Universal;

        public final String lowerCase;

        DataType() {
            switch (this.ordinal()) {
                case 0:
                    lowerCase = "java";
                    break;
                case 1:
                    lowerCase = "bedrock";
                    break;
                default:
                    lowerCase = "universal";
                    break;
            }
        }
    }
}
