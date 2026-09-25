package io.github.rutwoklabs.jbuildpilot.downloader;

import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Artifact repository that resolves bare tool identities ("maven", "gradle", "java") to a
 * concrete, pinned version served from an official HTTPS source, together with the exact
 * checksum published by that source.
 *
 * <p>Pinning is the trust anchor: the expected hash is baked into the binary (not fetched at
 * runtime), so a tampered mirror cannot substitute its own checksum. Values below are the
 * official published hashes. {@link ChecksumVerifier} auto-selects the digest by hash length,
 * so SHA-256 (Gradle) and SHA-512 (Maven) both verify.
 *
 * <p>Versioned ids ("maven-3.9.9", "gradle-8.10.2") and Maven-Central coordinates
 * ("group:artifact:version") are resolved to their canonical official URLs, mirroring
 * {@link TrustedOfficialRepository}.
 */
public class PinnedToolRepository implements ArtifactRepository {

    /** A pinned distribution: canonical versioned id, official URL, and published checksum. */
    private record Pin(String versionedId, URI uri, String checksum) { }

    // Pinned "known-good" distribution per bare tool. Checksums are the official published values.
    private static final Map<String, Pin> PINS = new LinkedHashMap<>();
    static {
        PINS.put("maven", new Pin(
                "maven-3.9.9",
                URI.create("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"),
                // official apache-maven-3.9.9-bin.zip.sha512
                "8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba"));
        PINS.put("gradle", new Pin(
                "gradle-8.10.2",
                URI.create("https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"),
                // official gradle-8.10.2-bin.zip.sha256
                "31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26"));
        // NOTE: "java" (JDK) is intentionally not pinned yet. JDK distributions are
        // platform-specific and the acquisition path only fires for MISSING/UNKNOWN Java,
        // which the host detector effectively never reports. Left unpinned (honest) rather
        // than shipping a hash we cannot keep correct across OS/arch.
    }

    private Pin pinFor(String identity) {
        if (identity == null) {
            return null;
        }
        // Accept both a bare tool ("maven") and an already-versioned id ("maven-3.9.9").
        int dash = identity.indexOf('-');
        String base = dash > 0 ? identity.substring(0, dash) : identity;
        return PINS.get(base);
    }

    @Override
    public Artifact toArtifact(String identity, VersionConstraint versionConstraint) {
        Pin pin = pinFor(identity);
        if (pin != null) {
            return new Artifact(pin.versionedId(), pin.checksum());
        }
        // Unknown tool: hand back a null-checksum artifact so the caller reports it clearly
        // and the checksum verifier rejects any unverifiable download.
        return new Artifact(identity, null);
    }

    @Override
    public String resolveChecksum(String identity, VersionConstraint versionConstraint) {
        Pin pin = pinFor(identity);
        return pin != null ? pin.checksum() : null;
    }

    @Override
    public URI resolveUri(Artifact artifact) {
        String id = artifact.getId();

        // Fast path: exact pinned versioned id.
        for (Pin pin : PINS.values()) {
            if (pin.versionedId().equals(id)) {
                return pin.uri();
            }
        }

        // Generic official mappings (same shape as TrustedOfficialRepository).
        if (id.startsWith("maven-")) {
            String version = id.substring(6);
            return URI.create("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/"
                    + version + "/apache-maven-" + version + "-bin.zip");
        } else if (id.startsWith("gradle-")) {
            String version = id.substring(7);
            return URI.create("https://services.gradle.org/distributions/gradle-" + version + "-bin.zip");
        } else {
            String[] parts = id.split(":");
            if (parts.length == 3) {
                String group = parts[0].replace('.', '/');
                String name = parts[1];
                String version = parts[2];
                return URI.create("https://repo1.maven.org/maven2/" + group + "/" + name + "/"
                        + version + "/" + name + "-" + version + ".jar");
            }
        }

        throw new IllegalArgumentException("No pinned or official distribution known for: " + id);
    }
}
