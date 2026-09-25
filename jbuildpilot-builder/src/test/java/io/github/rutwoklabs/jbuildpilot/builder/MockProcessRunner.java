package io.github.rutwoklabs.jbuildpilot.builder;

import java.nio.file.Path;
import java.util.List;

public class MockProcessRunner implements ProcessRunner {
    
    private List<String> lastCommand;
    private Path lastDirectory;
    private Path lastJavaHome;

    @Override
    public BuildResult run(List<String> command, Path directory, Path javaHome) {
        this.lastCommand = command;
        this.lastDirectory = directory;
        this.lastJavaHome = javaHome;
        return new BuildResult(0, "mock-stdout", "mock-stderr");
    }

    public List<String> getLastCommand() {
        return lastCommand;
    }

    public Path getLastDirectory() {
        return lastDirectory;
    }

    public Path getLastJavaHome() {
        return lastJavaHome;
    }
}
