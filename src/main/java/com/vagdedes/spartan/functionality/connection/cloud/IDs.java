package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.api.API;

import java.util.Objects;

public class IDs {

    public static final String resource = "%%__RESOURCE__%%";
    public static final boolean enabled = AlgebraUtils.validInteger(resource);
    private static String
            user = "%%__USER__%%",
            file = "%%__NONCE__%%";

    public static final boolean
            hasUserIDByDefault = !user.startsWith("%%__");

    private static int platform = 0;

    static {
        if (!file.startsWith("%%__") && !AlgebraUtils.validInteger(file)) {
            file = String.valueOf(Objects.hash(file));
        }
    }

    // Setters

    static void set(int user, int nonce) {
        IDs.user = Integer.toString(user);
        IDs.file = Integer.toString(nonce);
        CloudBase.clear(false);
    }

    public static void setPlatform(int id) {
        platform = id;
    }

    // IDs

    public static String user() {
        return user;
    }

    public static String file() {
        return !IDs.enabled ? (CloudBase.hasToken() ? Integer.toString(CloudBase.getRawToken().hashCode()) : user) : file;
    }

    static String platform() {
        return IDs.isBuiltByBit() ? "BuiltByBit" : IDs.isPolymart() ? "Polymart" : "SpigotMC";
    }

    // Platforms

    public static boolean canAdvertise() {
        return !IDs.enabled || IDs.isBuiltByBit() || IDs.isPolymart();
    }

    public static boolean isBuiltByBit() {
        return platform == 2 || "%%__FILEHASH__%%".length() != 16;
    }

    public static boolean isPolymart() {
        return platform == 3 || "%%__POLYMART__%%".length() == 1;
    }

    public static String hide(String id) {
        try {
            double version = Double.parseDouble(API.getVersion().substring(6)),
                    number = AlgebraUtils.cut(Integer.parseInt(id) / version, 6);
            return String.valueOf(number).replace("-", "*").replace(".", "-");
        } catch (Exception ex) {
            return "0";
        }
    }

}
