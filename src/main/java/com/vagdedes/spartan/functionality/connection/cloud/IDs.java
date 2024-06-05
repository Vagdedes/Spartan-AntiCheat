package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.api.API;

import java.util.Objects;

public class IDs {

    private static final String
            user = "%%__USER__%%",
            defaultLocalID = "0";
    private static String
            nonce = "%%__NONCE__%%",
            localID = defaultLocalID;

    public static final boolean
            hasUserIDByDefault = !user.startsWith("%%__");

    private static int platform = 0;

    static {
        if (!nonce.startsWith("%%__") && !AlgebraUtils.validInteger(nonce)) {
            nonce = String.valueOf(Objects.hash(nonce));
        }
    }

    static void setUserID(int id) {
        localID = Integer.toString(id);
        Config.refreshFields(false);
    }

    // Platforms

    public static String user() {
        return !JarVerification.enabled ? localID : user;
    }

    public static String nonce() {
        return !JarVerification.enabled ? (CloudBase.hasToken() ? Integer.toString(CloudBase.getRawToken().hashCode()) : localID) : nonce;
    }

    static String resource() {
        return !JarVerification.enabled ? localID : "%%__RESOURCE__%%";
    }

    // Platforms

    public static void setPlatform(int id) {
        platform = id;
    }

    public static boolean isBuiltByBit() {
        return platform == 2 || "%%__FILEHASH__%%".length() != 16;
    }

    public static boolean isPolymart() {
        return platform == 3 || "%%__POLYMART__%%".length() == 1;
    }

    public static boolean isSpigotMC() {
        return platform == 1 || JarVerification.enabled && !CloudBase.hasToken() && !isBuiltByBit() && !isPolymart();
    }

    public static String getPlatform(boolean notNull) {
        return IDs.isBuiltByBit() ? "BuiltByBit" : IDs.isPolymart() ? "Polymart" : notNull || isSpigotMC() ? "SpigotMC" : null;
    }

    public static String hide(String id) {
        double version = Double.parseDouble(API.getVersion().substring(6));
        double number = AlgebraUtils.cut(Integer.parseInt(id) / version, 6);
        return String.valueOf(number).replace("-", "*").replace(".", "-");
    }

}
