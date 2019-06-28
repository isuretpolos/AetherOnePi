package de.isuret.polos.AetherOnePi.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public static String get(String urlPath) throws Exception {

        StringBuilder result = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        String line;

        while ((line = rd.readLine()) != null) {
            result.append(line + "\n");
        }

        rd.close();

        return result.toString().trim();
    }
}
