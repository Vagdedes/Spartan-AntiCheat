package me.vagdedes.spartan.objects.features;

import io.signality.utils.system.Events;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;

public class IncompatibleItem {

    private final Events.EventType eventType;
    private final Material material;
    private final String name;
    private final Enums.HackType[] hackTypes;
    private int seconds;

    public IncompatibleItem(Events.EventType eventType, Material material, String name, Enums.HackType[] hackTypes) {
        this.eventType = eventType;
        this.material = material;
        this.name = name.replace(" ", "%spc%");
        this.hackTypes = hackTypes;
        this.seconds = 0;
    }

    public String getConfigurationKey() {
        StringBuilder checks = new StringBuilder();

        if (hackTypes.length > 0) {
            for (Enums.HackType hackType : hackTypes) {
                checks.append(hackType.toString()).append("|");
            }
            checks = new StringBuilder(checks.substring(0, checks.length() - 1));
        } else {
            checks = new StringBuilder("unknown");
        }
        return eventType.toString() + " " + material.toString().toLowerCase().replace("_", "-") + " " + name + " " + checks;
    }

    public Events.EventType getEventType() {
        return eventType;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        String space = "%spc%";
        return name.equalsIgnoreCase(space) ? null : name.replace(space, " ");
    }

    public String getActualName() {
        return name;
    }

    public Enums.HackType[] getHackTypes() {
        return hackTypes;
    }

    public int getSeconds() {
        return Math.max(1, seconds);
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
