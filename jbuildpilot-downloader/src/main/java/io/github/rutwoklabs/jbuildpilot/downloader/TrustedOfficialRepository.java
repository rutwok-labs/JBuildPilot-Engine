package io.github.rutwoklabs.jbuildpilot.downloader;

import java.net.URI;

/**
 * A genuine artifact repository resolving strictly against trusted official sources.
 */
public class TrustedOfficialRepository implements ArtifactRepository {

    @Override
    public URI resolveUri(Artifact artifact) {
        String id = artifact.getId();
        
        // Example IDs: 
        // maven-3.9.6 -> https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
        // gradle-8.5 -> https://services.gradle.org/distributions/gradle-8.5-bin.zip
        
        if (id.startsWith("maven-")) {
            String version = id.substring(6);
            return URI.create("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/" + version + "/apache-maven-" + version + "-bin.zip");
        } else if (id.startsWith("gradle-")) {
            String version = id.substring(7);
            return URI.create("https://services.gradle.org/distributions/gradle-" + version + "-bin.zip");
        } else {
            // Treat as a standard java dependency mapping for Maven Central
            // e.g. "com.google.guava:guava:31.0.1-jre"
            String[] parts = id.split(":");
            if (parts.length == 3) {
                String group = parts[0].replace('.', '/');
                String name = parts[1];
                String version = parts[2];
                return URI.create("https://repo1.maven.org/maven2/" + group + "/" + name + "/" + version + "/" + name + "-" + version + ".jar");
            }
        }
        
        throw new IllegalArgumentException("Unknown artifact format for trusted repository resolution: " + id);
    }
}
