package dev.cwhead.GravesX.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client for Toptal's Hastebin service.
 * API docs: https://www.toptal.com/developers/hastebin/documentation
 */
public final class ToptalUtil {

    private static final String API_URL   = "https://hastebin.com/documents";
    private static final String VIEW_BASE = "https://hastebin.com/";
    private static final int MAX_LEN      = 400_000;

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;
    private static final String USER_AGENT      = "GravesX/1 ToptalUtil (+hastebin)";

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
        if (content.length() > MAX_LEN) return null;

        HttpURLConnection conn = null;
        try {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            setup(conn);
            if (bearerToken != null && !bearerToken.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
            }
            conn.setFixedLengthStreamingMode(data.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            final int code = conn.getResponseCode();
            final boolean ok = code >= 200 && code < 300;

            try (InputStream is = ok ? conn.getInputStream()
                    : (conn.getErrorStream() != null ? conn.getErrorStream() : InputStream.nullInputStream());
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                if (!ok) return null;

                String key = extractKey(sb.toString());
                return (key != null && !key.isBlank()) ? VIEW_BASE + key : null;
            }
        } catch (IOException ignored) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void setup(HttpURLConnection conn) throws ProtocolException {
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
    }

    private static String extractKey(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"key\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = (q1 >= 0) ? json.indexOf('"', q1 + 1) : -1;
        if (q1 >= 0 && q2 > q1) {
            return json.substring(q1 + 1, q2).trim();
        }
        return null;
    }
}