package io.github.rutwoklabs.jbuildpilot.downloader;

import java.util.Objects;

/**
 * Represents a remotely available resource to be acquired.
 */
public class Artifact {
    private final String id;
    private final String expectedChecksum;
    private final String safeFileName;

    public Artifact(String id, String expectedChecksum) {
        this.id = Objects.requireNonNull(id);
        this.expectedChecksum = expectedChecksum;
        
        // Security: generate a safe filename immediately and never trust remote Content-Disposition
        // This converts characters like '/' or '\' to underscores to prevent path traversal on cache writes
        this.safeFileName = generateSafeFileName(id);
    }

    public String getId() { return id; }
    public String getExpectedChecksum() { return expectedChecksum; }
    public String getSafeFileName() { return safeFileName; }

    private String generateSafeFileName(String rawId) {
        // Strip everything except alphanumeric, underscore, and hyphens. 
        // We explicitly ban '.' to prevent ANY kind of traversal or extension trickery.
        return rawId.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".bin";
    }
}
