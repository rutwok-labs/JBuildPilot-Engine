package io.github.rutwoklabs.jbuildpilot.downloader;

import java.net.URI;

/**
 * Strategy interface to resolve Artifact identities into downloadable URIs.
 */
public interface ArtifactRepository {
    /**
     * Resolves an artifact to an absolute HTTPS URI.
     */
    URI resolveUri(Artifact artifact);

    /**
     * Resolves the expected checksum (SHA-256) for a given requirement.
     */
    default String resolveChecksum(String identity, io.github.rutwoklabs.jbuildpilot.core.VersionConstraint versionConstraint) {
        // Fallback for simple repositories; real implementations should fetch the .sha256 sidecar
        // or consult a trusted pinned manifest.
        return null;
    }

    /**
     * Builds the concrete {@link Artifact} to download for a bare tool identity (e.g. "maven",
     * "gradle", "java") under the given constraint. The default keeps the identity verbatim and
     * attaches whatever {@link #resolveChecksum} yields; repositories that pin concrete versions
     * (see PinnedToolRepository) override this to return a versioned id + pinned checksum.
     */
    default Artifact toArtifact(String identity, io.github.rutwoklabs.jbuildpilot.core.VersionConstraint versionConstraint) {
        return new Artifact(identity, resolveChecksum(identity, versionConstraint));
    }
}
