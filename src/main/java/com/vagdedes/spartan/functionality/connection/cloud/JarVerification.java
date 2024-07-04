package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class JarVerification {

    private static boolean valid = true;
    private static final String name = Register.plugin.getName();
    public static final boolean enabled = AlgebraUtils.validInteger("%%__RESOURCE__%%");

    static {
        if (!enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                }
            });
        }

        if (!isValid() && (enabled || CloudBase.hasToken())) {
            Register.disablePlugin();
        } else {
            SpartanEdition.refresh();
        }
    }

    private static boolean isValid() {
        boolean b = valid
                && name.equalsIgnoreCase("Spartan")
                && Register.plugin.getDescription().getAuthors().toString().startsWith("[Evangelos Dedes @Vagdedes");

        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website)
                    + "?" + CloudBase.identification + "&action=add&data=userVerification");

            if (results.length > 0) {
                String line = results[0];

                if (line.equalsIgnoreCase(String.valueOf(false))) {
                    return false;
                }
                if (CloudBase.hasToken() && AlgebraUtils.validInteger(line)) {
                    IDs.setPlatform(Integer.parseInt(line));
                }
            }
        } catch (Exception e) {
            if (SpartanBukkit.canAdvertise) {
                e.printStackTrace();
            }
        }
        return b;
    }

}