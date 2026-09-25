package io.github.rutwoklabs.jbuildpilot.downloader;

import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class PinnedToolRepositoryTest {

    private final PinnedToolRepository repo = new PinnedToolRepository();
    private final VersionConstraint any = new VersionConstraint("*");

    @Test
    void mavenBareIdentityResolvesToPinnedVersionedArtifact() {
        Artifact a = repo.toArtifact("maven", any);
        assertEquals("maven-3.9.9", a.getId());
        assertEquals(128, a.getExpectedChecksum().length(), "Maven pin is a SHA-512");
        assertEquals(
                "8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba",
                a.getExpectedChecksum());
    }

    @Test
    void gradleBareIdentityResolvesToPinnedVersionedArtifact() {
        Artifact a = repo.toArtifact("gradle", any);
        assertEquals("gradle-8.10.2", a.getId());
        assertEquals(64, a.getExpectedChecksum().length(), "Gradle pin is a SHA-256");
        assertEquals(
                "31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26",
                a.getExpectedChecksum());
    }

    @Test
    void alreadyVersionedIdentityStillResolvesToItsPin() {
        assertEquals("maven-3.9.9", repo.toArtifact("maven-3.9.9", any).getId());
        assertEquals("gradle-8.10.2", repo.toArtifact("gradle-8.10.2", any).getId());
    }

    @Test
    void unpinnedToolYieldsNullChecksumSoDownloadIsRefused() {
        Artifact a = repo.toArtifact("java", any);
        assertEquals("java", a.getId());
        assertNull(a.getExpectedChecksum());
        assertNull(repo.resolveChecksum("java", any));
    }

    @Test
    void resolveUriForPinnedMavenIsOfficialHttps() {
        URI uri = repo.resolveUri(repo.toArtifact("maven", any));
        assertEquals(
                "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
                uri.toString());
    }

    @Test
    void resolveUriForPinnedGradleIsOfficialHttps() {
        URI uri = repo.resolveUri(repo.toArtifact("gradle", any));
        assertEquals(
                "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip",
                uri.toString());
    }

    @Test
    void resolveUriForArbitraryVersionedMavenFollowsOfficialPattern() {
        URI uri = repo.resolveUri(new Artifact("maven-3.9.6", null));
        assertEquals(
                "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip",
                uri.toString());
    }

    @Test
    void resolveUriForMavenCentralCoordinate() {
        URI uri = repo.resolveUri(new Artifact("org.junit.jupiter:junit-jupiter-api:5.10.0", null));
        assertEquals(
                "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.0/junit-jupiter-api-5.10.0.jar",
                uri.toString());
    }

    @Test
    void resolveUriForUnknownIdentityThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.resolveUri(new Artifact("not-a-tool", null)));
    }
}
