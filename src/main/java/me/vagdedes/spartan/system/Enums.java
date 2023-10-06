package me.vagdedes.spartan.system;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.objects.system.CancelCause;
import me.vagdedes.spartan.objects.system.Check;

import java.util.Map;
import java.util.UUID;

public class Enums {

    // Should have used capital letters but won't change them now so to not break the dozen APIs who use these enums
    public enum HackType {
        XRay, Exploits, EntityMove, NoSwing, IrregularMovements, ImpossibleActions, ItemDrops, AutoRespawn, InventoryClicks,
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
                Map<UUID, CancelCause> disabledUsers = this.check.copyDisabledUsers();
                Map<UUID, CancelCause> silentUsers = this.check.copySilentUsers();
                Map<Integer, Integer> maxCancelledViolations = this.check.copyMaxCancelledViolations();
                this.check.clearCache();
                this.check = new Check(this, disabledUsers, silentUsers, maxCancelledViolations);
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

    public enum PunishmentCategory {
        UNLIKE, POTENTIAL, CERTAIN, DEFINITE, ABSOLUTE;

        private final String string;
        private final double multiplier;

        PunishmentCategory() {
            string = this.name().toLowerCase();

            switch (this.ordinal()) {
                case 0:
                    multiplier = 1.0;
                    break;
                case 1:
                    multiplier = 2.0;
                    break;
                case 2:
                    multiplier = 3.5;
                    break;
                case 3:
                    multiplier = 5.5;
                    break;
                default:
                    multiplier = 8.0;
                    break;
            }
        }

        @Override
        public String toString() {
            return string;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    public enum Permission {
        CONDITION, REPORT, STAFF_CHAT, WAVE, RECONNECT, ADMIN, RELOAD,
        KICK, BYPASS, MANAGE, INFO, CHAT_PROTECTION, WARN, USE_BYPASS,
        BAN, UNBAN, NOTIFICATIONS;

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
}
