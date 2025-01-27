package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import me.gwndaan.printer.PrinterModeAPI;

public class PrinterMode {

    public static boolean isUsing(PlayerProtocol p) {
        return Compatibility.CompatibilityType.PRINTER_MODE.isFunctional()
                && PrinterModeAPI.isInPrinterMode(p.bukkit());
    }
}
