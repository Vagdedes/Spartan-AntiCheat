package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.gwndaan.printer.PrinterModeAPI;

public class PrinterMode {

    public static boolean isUsing(SpartanProtocol p) {
        return Compatibility.CompatibilityType.PRINTER_MODE.isFunctional()
                && PrinterModeAPI.isInPrinterMode(p.bukkit);
    }
}
