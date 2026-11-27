package dev.cwhead.GravesX.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Minimal Pastebin API client for posting text dumps.
 * Returns the paste URL on success, or {@code null} on failure.
 */
public final class PastebinUtil {

    private static final String API_URL = "https://pastebin.com/api/api_post.php";

    private PastebinUtil() {}

    /**
     * Post text content to Pastebin.
     *
     * @param devKey   Pastebin API dev key (required)
     * @param userKey  Pastebin user key (optional; required for PRIVATE pastes)
     * @param title    Paste title
     * @param content  Paste body (text)
     * @param privacy  "PUBLIC" | "UNLISTED" | "PRIVATE" (case-insensitive)
     * @param expire   "N" | "10M" | "1H" | "1D" | "1W" | "2W" | "1M" | "6M" | "1Y"
     * @return The paste URL on success; {@code null} on error.
     */
    public static String post(String devKey,
                              String userKey,
                              String title,
                              String content,
                              String privacy,
                              String expire) {

        if (devKey == null || devKey.isBlank()) {
            return null;
        }

        int pastePrivate = mapPrivacyToInt(privacy); // 0 public, 1 unlisted, 2 private
        String exp = (expire == null || expire.isBlank()) ? "1W" : expire.toUpperCase(Locale.ROOT);
        String safeTitle = (title == null || title.isBlank())
                ? "GravesX Dump " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : title;

        HttpURLConnection conn = null;
        try {
            StringBuilder body = new StringBuilder();
            append(body, "api_dev_key", devKey);
            append(body, "api_option", "paste");
            append(body, "api_paste_code", content == null ? "" : content);
            append(body, "api_paste_private", String.valueOf(pastePrivate));
            append(body, "api_paste_name", safeTitle);
            append(body, "api_paste_expire_date", exp);
            append(body, "api_paste_format", "text");
            if (userKey != null && !userKey.isBlank()) {
                append(body, "api_user_key", userKey);
            }

            byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/plain");
            conn.setRequestProperty("User-Agent", "GravesX/1 PastebinUtil");
            conn.setFixedLengthStreamingMode(out.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) stream = InputStream.nullInputStream();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String resp = br.readLine();
                if (code >= 200 && code < 300 && resp != null && resp.startsWith("http")) {
                    return resp.trim();
                }
                return null;
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static int mapPrivacyToInt(String privacy) {
        String p = safeUpper(privacy, "UNLISTED");
        if ("PUBLIC".equals(p))  return 0;
        if ("PRIVATE".equals(p)) return 2;
        return 1;
    }

    private static void append(StringBuilder sb, String key, String val) {
        if (!sb.isEmpty()) sb.append('&');
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(val == null ? "" : val, StandardCharsets.UTF_8));
    }

    private static String safeUpper(String in, String dflt) {
        return (in == null || in.isBlank()) ? dflt : in.toUpperCase(Locale.ROOT);
    }
}