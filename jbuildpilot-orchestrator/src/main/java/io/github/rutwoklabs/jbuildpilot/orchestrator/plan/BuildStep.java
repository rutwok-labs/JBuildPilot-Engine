package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.builder.BuildEngine;
import io.github.rutwoklabs.jbuildpilot.builder.BuildRequest;
import io.github.rutwoklabs.jbuildpilot.builder.BuildResult;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

import java.util.Collections;

public class BuildStep implements ExecutionStep {
    private final String buildSystem;

    public BuildStep(String buildSystem) {
        this.buildSystem = buildSystem;
    }

    @Override
    public String getDescription() {
        return "Build Project (" + buildSystem + ")";
    }

    @Override
    public void execute(EngineContext context) {
        // Explicitly check EXECUTE_BUILD permission
        PermissionDecision decision = context.getPermissionManager().requestPermission(
                new PermissionRequest(PermissionType.EXECUTE_BUILD, "Execute build via " + buildSystem)
        );
        
        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission denied to execute build.");
        }
        
        // Execute the build via Phase 8 abstractions
        BuildEngine engine = context.getBuildEngine(buildSystem);
        if (engine != null) {
            // "build" is a valid Gradle task but not a Maven lifecycle phase; Maven uses "package".
            String buildGoal = buildSystem != null && buildSystem.toLowerCase().contains("gradle")
                    ? "build"
                    : "package";
            BuildRequest req = new BuildRequest(context.getProjectDirectory(), Collections.singletonList(buildGoal), context.getJavaHome(), null, true);
            BuildResult result = engine.execute(req);
            context.log("Build executed with exit code: " + result.getExitCode());
            if (result.getExitCode() != 0) {
                context.log("Build error output: " + result.getStderr());
                throw new RuntimeException("Build failed via " + buildSystem + ". Exit code: " + result.getExitCode());
            }
        } else {
            context.log("Simulating build execution for: " + buildSystem);
        }
    }
}
