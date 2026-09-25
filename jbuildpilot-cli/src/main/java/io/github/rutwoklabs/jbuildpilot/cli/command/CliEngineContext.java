package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.builder.BuildEngine;
import io.github.rutwoklabs.jbuildpilot.builder.DefaultProcessRunner;
import io.github.rutwoklabs.jbuildpilot.builder.MavenBuildEngine;
import io.github.rutwoklabs.jbuildpilot.builder.GradleBuildEngine;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

import java.nio.file.Path;

public class CliEngineContext implements EngineContext {

    private final PermissionManager permissionManager;

    public CliEngineContext(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    @Override
    public BuildEngine getBuildEngine(String system) {
        if ("maven".equalsIgnoreCase(system)) {
            return new MavenBuildEngine(permissionManager, new DefaultProcessRunner());
        } else if ("gradle".equalsIgnoreCase(system)) {
            return new GradleBuildEngine(permissionManager, new DefaultProcessRunner());
        }
        return null;
    }

    @Override
    public Path getProjectDirectory() {
        return io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir().toAbsolutePath().normalize();
    }

    @Override
    public Path getJavaHome() {
        return null; // Will fallback to system default in DefaultProcessRunner
    }

    @Override
    public void log(String message) {
        System.out.println("[ENGINE] " + message);
    }
}
