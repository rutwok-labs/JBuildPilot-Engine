package io.github.rutwoklabs.jbuildpilot.environment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Metadata state describing a managed project space.
 */
public class EnvironmentManifest {
    private final String projectId;
    private final String javaVersion;
    private final String buildSystem;

    public EnvironmentManifest(String projectId, String javaVersion, String buildSystem) {
        this.projectId = projectId;
        this.javaVersion = javaVersion;
        this.buildSystem = buildSystem;
    }

    public String getProjectId() { return projectId; }
    public String getJavaVersion() { return javaVersion; }
    public String getBuildSystem() { return buildSystem; }

    public void writeTo(Path path) throws IOException {
        Properties props = new Properties();
        props.setProperty("projectId", projectId != null ? projectId : "");
        props.setProperty("javaVersion", javaVersion != null ? javaVersion : "");
        props.setProperty("buildSystem", buildSystem != null ? buildSystem : "");

        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "JBuildPilot Managed Environment Manifest");
        }
    }

    public static EnvironmentManifest readFrom(Path path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }
        return new EnvironmentManifest(
                props.getProperty("projectId", ""),
                props.getProperty("javaVersion", ""),
                props.getProperty("buildSystem", "")
        );
    }
}
