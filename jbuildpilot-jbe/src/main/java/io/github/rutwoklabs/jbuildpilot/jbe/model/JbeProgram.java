package io.github.rutwoklabs.jbuildpilot.jbe.model;

import java.util.List;

public class JbeProgram {
    private final String projectName;
    private final String javaVersion;
    private final String buildSystem;
    private final String mainClass;
    private final List<String> dependencies;

    public JbeProgram(String projectName, String javaVersion, String buildSystem, String mainClass, List<String> dependencies) {
        this.projectName = projectName;
        this.javaVersion = javaVersion;
        this.buildSystem = buildSystem;
        this.mainClass = mainClass;
        this.dependencies = dependencies;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getBuildSystem() {
        return buildSystem;
    }

    public String getMainClass() {
        return mainClass;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
