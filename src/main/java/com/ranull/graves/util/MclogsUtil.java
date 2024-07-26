package com.ranull.graves.util;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for posting data to mclo.gs.
 */
public final class MclogsUtil {

    private static final String API_URL = "https://api.mclo.gs/1/log";

    /**
     * Posts the given log content to mclo.gs and returns the URL of the posted log.
     *
     * @param content The log content to be posted.
     * @return The URL of the posted log, or null if the post was unsuccessful.
     */
    public static String postLogToMclogs(String content) {
        try {
            URL url = new URL(API_URL);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();

            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String urlParameters = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8.toString());

            try (DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream())) {
                dataOutputStream.writeBytes(urlParameters);
                dataOutputStream.flush();
            }

            StringBuilder response;
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.getBoolean("success")) {
                return jsonResponse.getString("url");
            } else {
                // System.out.println("Log upload failed. Error: " + jsonResponse.getString("error"));
                return null;
            }
        } catch (IOException | JSONException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}