package com.vagdedes.spartan.handlers.connection;

import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class DiscordMemberCount {

    public static final String discordURL = "https://www.idealistic.ai/discord";
    private static int
            count = 0,
            times = 0;

    public static void ignore() {
        times += 1;
    }

    public static int get() {
        if (times == 0) {
            times = 100;

            SpartanBukkit.connectionThread.execute(() -> {
                try {
                    String number = RequestUtils.get(discordURL + "/count/")[0];

                    if (AlgebraUtils.validInteger(number)) {
                        count = Integer.parseInt(number);
                    } else {
                        count = 0;
                    }
                } catch (Exception ex) {
                    count = 0;
                }
            });
        } else {
            times -= 1;
        }
        return count;
    }
}
