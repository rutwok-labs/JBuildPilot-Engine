package io.github.rutwoklabs.jbuildpilot.builder;

public interface BuildEngine {
    BuildResult execute(BuildRequest request);
}
