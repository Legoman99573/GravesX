package com.ranull.graves.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client for Toptal's Hastebin service.
 * API docs: https://www.toptal.com/developers/hastebin/documentation
 */
public final class ToptalUtil {

    private static final String API_URL = "https://hastebin.com/documents";
    private static final String VIEW_BASE = "https://hastebin.com/";
    private static final int MAX_LEN = 400_000;

    private ToptalUtil() {}

    /**
     * Post plain text to Toptal Hastebin.
     *
     * @param content Text to upload (400k chars or 1MB)
     * @param bearerToken Required token for Authorization header ("Bearer token");
     * @return The standard view URL (https://hastebin.com/{key}), or null on failure.
     */
    public static String post(String content, String bearerToken) {
        if (content == null || content.isEmpty()) return null;
        if (content.length() > MAX_LEN) {
            return null;
        }

        try {
            InputStream is = getInputStream(content, bearerToken);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                String json = sb.toString();

                String key = extractKey(json);
                if (key != null && !key.isEmpty()) {
                    return VIEW_BASE + key;
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static InputStream getInputStream(String content, String bearerToken) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        if (bearerToken != null && !bearerToken.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }
        conn.setFixedLengthStreamingMode(data.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        return is;
    }

    private static String extractKey(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"key\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = q1 >= 0 ? json.indexOf('"', q1 + 1) : -1;
        if (q1 >= 0 && q2 > q1) {
            return json.substring(q1 + 1, q2).trim();
        }
        return null;
    }
}
