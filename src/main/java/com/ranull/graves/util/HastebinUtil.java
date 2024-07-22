package com.ranull.graves.util;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for posting data to Hastebin.
 */
public final class HastebinUtil {

    /**
     * Posts the given data to Hastebin and returns the URL of the posted data.
     *
     * @param data The data to be posted.
     * @param raw  Whether to return the raw URL or the formatted URL.
     * @return The URL of the posted data, or null if the post was unsuccessful.
     */
    public static String postDataToHastebin(String data, boolean raw) {
        String urlString = "https://www.toptal.com/developers/hastebin/documents/";
        String pasteRawURLString = "https://www.toptal.com/developers/hastebin/raw/";
        String pasteURLString = "https://www.toptal.com/developers/hastebin/";

        try {
            URL url = new URL(urlString);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();

            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestMethod("POST");

            try (DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream())) {
                dataOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
            }

            String response;
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
                response = bufferedReader.readLine();
            }

            if (response != null && response.contains("\"key\"")) {
                response = response.substring(response.indexOf(":") + 2, response.length() - 2);
                response = (raw ? pasteRawURLString : pasteURLString) + response;
            }

            return !response.equals(urlString) ? response : null;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}