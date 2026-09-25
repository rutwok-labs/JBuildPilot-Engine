package io.github.rutwoklabs.jbuildpilot.security;

import io.github.rutwoklabs.jbuildpilot.environment.PathManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ApiKeyProvider {
    private final PathManager pathManager;
    private static final String ENV_VAR = "JBUILDPILOT_METADEFENDER_APIKEY";

    public ApiKeyProvider(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    public String getApiKey() {
        // 1. Try environment variable
        String envKey = System.getenv(ENV_VAR);
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. Try ~/.jbuildpilot/config.properties
        Path configPath = pathManager.resolveSafely(pathManager.getHomeDirectory(), "config.properties");
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                Properties props = new Properties();
                props.load(is);
                String propKey = props.getProperty("metadefender.apikey");
                if (propKey != null && !propKey.trim().isEmpty()) {
                    return propKey.trim();
                }
            } catch (Exception ignored) {
                // Ignore read errors, fall through to exception
            }
        }

        throw new IllegalStateException(
                "MetaDefender API Key is missing. Please set the '" + ENV_VAR + "' environment variable " +
                "or add 'metadefender.apikey' to " + configPath
        );
    }
}
