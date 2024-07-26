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
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String urlParameters = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8.toString());

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.writeBytes(urlParameters);
                out.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            StringBuilder response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.getBoolean("success")) {
                return jsonResponse.getString("url");
            } else {
                System.out.println("Log upload failed. Error: " + jsonResponse.getString("error"));
                return null;
            }
        } catch (IOException | JSONException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}