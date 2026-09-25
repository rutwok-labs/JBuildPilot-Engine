package io.github.rutwoklabs.jbuildpilot.orchestrator.engine;

import io.github.rutwoklabs.jbuildpilot.builder.BuildEngine;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;

import java.nio.file.Path;

public interface EngineContext {
    PermissionManager getPermissionManager();
    BuildEngine getBuildEngine(String system);
    Path getProjectDirectory();
    Path getJavaHome();
    void log(String message);
}
