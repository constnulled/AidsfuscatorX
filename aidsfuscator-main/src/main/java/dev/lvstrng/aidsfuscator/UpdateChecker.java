package dev.lvstrng.aidsfuscator;

import com.google.gson.JsonParser;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {
    private static final int TIMEOUT_MS = 4000;

    private static String getLatestReleaseTag() throws IOException {
        var connection = (HttpURLConnection) URI.create(
                "https://api.github.com/repos/LvStrnggg/aidsfuscator/releases/latest"
        ).toURL().openConnection();

        try {
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Logger.error("Update check failed: server returned HTTP %d", connection.getResponseCode());
                return null;
            }

            try (var is = connection.getInputStream()) {
                var response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return JsonParser.parseString(response).getAsJsonObject().get("tag_name").getAsString();
            }
        } finally {
            connection.disconnect();
        }
    }

    public static void checkAndPrintUpdates() {
        var thread = new Thread(() -> {
            try {
                var latest = getLatestReleaseTag();
                if (latest == null)
                    return;

                var current = AidsfuscatorInfo.build();
                if (!latest.equals(current)) {
                    Logger.warn("Running %s (Outdated). Please update to the latest version (%s)", AidsfuscatorInfo.versionText(), latest);
                    return;
                }

                Logger.info("Running %s (Up to date)", AidsfuscatorInfo.versionText());
            } catch (IOException e) {
                Logger.error("There was an error checking for the latest version of Aidsfuscator, running anyway", e);
            }
        });
        thread.setDaemon(true);
        thread.setName("update-checker");
        thread.start();
    }
}
