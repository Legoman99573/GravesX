package dev.cwhead.GravesX.util;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for posting data to mclo.gs.
 */
public final class MclogsUtil {

    private static final String API_URL = "https://api.mclo.gs/1/log";

    private MclogsUtil() {}

    /**
     * Posts the given log content to mclo.gs and returns the URL of the posted log.
     *
     * @param content The log content to be posted.
     * @return The URL of the posted log, or null if the post was unsuccessful.
     */
    public static String postLogToMclogs(String content) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);

            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "GravesX/1 (+https://mclo.gs/)");

            String safeContent = content == null ? "" : content;
            String urlParameters = "content=" + URLEncoder.encode(safeContent, StandardCharsets.UTF_8.name());

            try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
                dataOutputStream.write(urlParameters.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
            }

            int responseCode = connection.getResponseCode();

            boolean ok = responseCode >= 200 && responseCode < 300;
            try (InputStream is = ok ? connection.getInputStream() : connection.getErrorStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         is != null ? is : InputStream.nullInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                if (!ok) {
                    throw new IOException("HTTP " + responseCode + " Error response: " + sb);
                }

                JSONObject jsonResponse = new JSONObject(sb.toString());
                if (jsonResponse.optBoolean("success", false)) {
                    return jsonResponse.optString("url", null);
                } else {
                    String err = jsonResponse.optString("error", "unknown");
                    throw new IOException("Log upload failed. Error: " + err);
                }
            }
        } catch (IOException | JSONException exception) {
            exception.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}