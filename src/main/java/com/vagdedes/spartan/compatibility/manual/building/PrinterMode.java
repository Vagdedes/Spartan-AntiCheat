package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.gwndaan.printer.PrinterModeAPI;

public class PrinterMode {

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.PrinterMode.isFunctional() && PrinterModeAPI.isInPrinterMode(p.getPlayer());
    }
}
