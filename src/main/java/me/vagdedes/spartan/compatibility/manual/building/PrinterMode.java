package me.vagdedes.spartan.compatibility.manual.building;

import me.gwndaan.printer.PrinterModeAPI;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class PrinterMode {

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.PrinterMode.isFunctional() && PrinterModeAPI.isInPrinterMode(p.getPlayer());
    }
}
