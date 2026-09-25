package io.github.rutwoklabs.jbuildpilot.builder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class BuildRequest {
    private final Path projectDirectory;
    private final List<String> arguments;
    private final Path javaHome;
    private final Path toolHome;
    private final boolean useWrapper;

    public BuildRequest(Path projectDirectory, List<String> arguments, Path javaHome, Path toolHome, boolean useWrapper) {
        this.projectDirectory = projectDirectory;
        this.arguments = Collections.unmodifiableList(arguments);
        this.javaHome = javaHome;
        this.toolHome = toolHome;
        this.useWrapper = useWrapper;
    }

    public Path getProjectDirectory() {
        return projectDirectory;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public Path getJavaHome() {
        return javaHome;
    }

    public Path getToolHome() {
        return toolHome;
    }

    public boolean isUseWrapper() {
        return useWrapper;
    }
}
