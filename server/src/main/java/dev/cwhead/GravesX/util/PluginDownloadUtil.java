package dev.cwhead.GravesX.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Utility methods to download plugin jars from Spiget and place them in the server's plugins folder.
 *
 * <p>Note: This class uses simple HTTP requests with sensible timeouts and follows redirects.</p>
 */
public final class PluginDownloadUtil {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 8192;
    private static final String USER_AGENT = "GravesX/1 PluginDownloadUtil (+spiget)";

    private PluginDownloadUtil() {}

    /**
     * Downloads a plugin from Spiget and saves it to the plugins folder, replacing it if it exists.
     *
     * @param pluginId      The Spigot resource ID of the plugin.
     * @param pluginName    The name of the plugin file (without the ".jar" extension).
     * @param pluginsFolder The path to the plugins' folder.
     * @throws IOException If the download or file operations fail.
     */
    public static void downloadAndReplacePlugin(long pluginId, String pluginName, String pluginsFolder, CommandSender commandSender) throws IOException {
        downloadAndReplacePlugin(String.valueOf(pluginId), pluginName, pluginsFolder, commandSender);
    }

    /**
     * Downloads a plugin from Spiget and saves it to the plugins folder, replacing it if it exists.
     *
     * @param pluginId      The Spigot resource ID of the plugin.
     * @param pluginName    The name of the plugin file (without the ".jar" extension).
     * @param pluginsFolder The path to the plugins' folder.
     * @throws IOException If the download or file operations fail.
     */
    public static void downloadAndReplacePlugin(String pluginId, String pluginName, String pluginsFolder, CommandSender commandSender) throws IOException {
        String downloadUrl = "https://api.spiget.org/v2/resources/" + pluginId + "/download";
        File pluginsDir = new File(pluginsFolder);
        File pluginJar = new File(pluginsDir, pluginName + ".jar");

        Files.createDirectories(pluginsDir.toPath());

        sendMessageToExecutor(commandSender, ChatColor.GREEN + "Downloading plugin: " + pluginName + "...");
        File tempFile = downloadFile(downloadUrl, commandSender);
        if (tempFile == null) return;

        if (pluginJar.exists()) {
            sendMessageToExecutor(commandSender, ChatColor.GREEN + "Replacing existing plugin: " + pluginJar.getName());
            Files.delete(pluginJar.toPath());
        } else {
            sendMessageToExecutor(commandSender, ChatColor.GREEN + "Adding new plugin: " + pluginJar.getName());
        }

        Files.move(tempFile.toPath(), pluginJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        sendMessageToExecutor(commandSender,
                ChatColor.GREEN + "Plugin " + pluginName + " successfully updated in \"" + pluginsFolder + "\". Restart the server in order to take effect.");
    }

    /**
     * Downloads a file from the given URL to a temporary file.
     *
     * @param fileUrl The URL to download the file from.
     * @return A temporary file containing the downloaded data.
     * @throws IOException If the download fails.
     */
    private static File downloadFile(String fileUrl, CommandSender commandSender) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = open(fileUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode / 100 != 2) {
                sendMessageToExecutor(commandSender, ChatColor.RED + "Failed to download file. HTTP Response Code: " + responseCode);
                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) es.transferTo(DISCARD);
                } catch (IOException ignored) {}
                return null;
            }

            File tempFile = File.createTempFile("plugin-", ".jar");
            tempFile.deleteOnExit();
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Opens an HTTP connection with headers, timeouts, and redirect handling.
     */
    private static HttpURLConnection open(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setup(conn);

        int status = conn.getResponseCode();
        int redirects = 0;

        while (isRedirect(status) && redirects < 5) {
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            if (loc == null || loc.isBlank()) throw new ProtocolException("Redirect without Location header");
            URL newUrl = new URL(url, loc);
            conn = (HttpURLConnection) newUrl.openConnection();
            setup(conn);
            status = conn.getResponseCode();
            redirects++;
        }
        return conn;
    }

    private static void setup(HttpURLConnection conn) throws ProtocolException {
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/java-archive, application/octet-stream");
    }

    private static boolean isRedirect(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == 307
                || code == 308;
    }

    private static void sendMessageToExecutor(CommandSender commandSender, String message) {
        if (commandSender != null && message != null) {
            commandSender.sendMessage(message);
        }
    }

    /**
     * Sink to swallow streams when only status matters.
     */
    private static final OutputStream DISCARD = new OutputStream() {
        @Override
        public void write(int b) {}
        @Override
        public void write(byte @NotNull [] b, int off, int len) {}
        @Override
        public void write(byte @NotNull [] b) {}
    };
}