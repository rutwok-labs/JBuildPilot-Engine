package io.github.rutwoklabs.jbuildpilot.downloader;

import java.util.Objects;

public class DownloadRequest {
    private final Artifact artifact;
    private final ArtifactRepository repository;

    public DownloadRequest(Artifact artifact, ArtifactRepository repository) {
        this.artifact = Objects.requireNonNull(artifact);
        this.repository = Objects.requireNonNull(repository);
    }

    public Artifact getArtifact() { return artifact; }
    public ArtifactRepository getRepository() { return repository; }
}
