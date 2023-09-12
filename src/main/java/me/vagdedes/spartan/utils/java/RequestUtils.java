package me.vagdedes.spartan.utils.java;

import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RequestUtils {

    public static final int
            defaultTimeOut = 30_000,
            minimumTimeOut = defaultTimeOut / 10;

    public static String[] get(String link, String method, String message, int timeOut) throws Exception {
        boolean isPost = method.equals("POST");
        String[] split = link.split(" ");

        String token = CloudFeature.getToken();
        URL url = new URL(split[0]);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.addRequestProperty("User-Agent",
                (token != null ? token : SpartanEdition.getProductName(true))
                        + (message != null ? " (" + message + ")" : ""));
        connection.setRequestMethod(method);
        connection.setReadTimeout(timeOut);
        connection.setDoOutput(true);
        connection.connect();

        if (isPost && split.length > 1) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(split[1]);
            wr.flush();
            wr.close();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        List<String> array = new ArrayList<>(50);
        String line;

        while ((line = in.readLine()) != null) {
            array.add(line);
        }
        in.close();
        return array.toArray(new String[0]);
    }

    public static String[] get(String link, String method) throws Exception {
        return get(link, method, null, defaultTimeOut);
    }

    public static String[] get(String link, int timeOut) throws Exception {
        return get(link, "GET", null, timeOut);
    }

    public static String[] get(String link) throws Exception {
        return get(link, "GET", null, defaultTimeOut);
    }

    public static String getDomain(String site, boolean secure) {
        site = site.replace((secure ? "https" : "http") + "://", "");

        for (int i = 0; i <= site.length(); i++) {
            if (site.substring(i).startsWith("/")) {
                return site.substring(0, i);
            }
        }
        return null;
    }
}
