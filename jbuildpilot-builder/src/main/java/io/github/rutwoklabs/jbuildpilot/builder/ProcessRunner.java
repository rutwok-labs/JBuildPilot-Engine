package io.github.rutwoklabs.jbuildpilot.builder;

import java.nio.file.Path;
import java.util.List;

public interface ProcessRunner {
    BuildResult run(List<String> command, Path directory, Path javaHome);
}
