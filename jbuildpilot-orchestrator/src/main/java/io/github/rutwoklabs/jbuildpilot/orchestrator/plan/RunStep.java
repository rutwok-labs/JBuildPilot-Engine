package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.builder.BuildEngine;
import io.github.rutwoklabs.jbuildpilot.builder.BuildRequest;
import io.github.rutwoklabs.jbuildpilot.builder.BuildResult;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

import java.util.Arrays;
import java.util.List;

public class RunStep implements ExecutionStep {
    private final String mainClass;
    private final String buildSystem;

    public RunStep(String mainClass, String buildSystem) {
        this.mainClass = mainClass;
        this.buildSystem = buildSystem;
    }

    @Override
    public String getDescription() {
        return "Run Main Class: " + mainClass + " via " + buildSystem;
    }

    @Override
    public void execute(EngineContext context) {
        PermissionDecision decision = context.getPermissionManager().requestPermission(
                new PermissionRequest(PermissionType.EXECUTE_PROCESS, "Run application main class " + mainClass)
        );
        
        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission denied to execute application.");
        }
        
        BuildEngine engine = context.getBuildEngine(buildSystem);
        if (engine != null) {
            List<String> args;
            if ("gradle".equals(buildSystem)) {
                args = Arrays.asList("run"); // Typically for gradle application plugin
            } else {
                args = Arrays.asList("exec:java", "-Dexec.mainClass=" + mainClass);
            }
            
            BuildRequest req = new BuildRequest(context.getProjectDirectory(), args, context.getJavaHome(), null, true);
            BuildResult result = engine.execute(req);
            
            if (result.getExitCode() != 0) {
                context.log("Run failed with exit code: " + result.getExitCode());
                context.log("Error output: " + result.getStderr());
                throw new RuntimeException("Application execution failed. Exit code: " + result.getExitCode());
            } else {
                context.log("Run executed successfully.");
                System.out.println(result.getStdout());
            }
        } else {
            throw new IllegalStateException("Build system not supported or found: " + buildSystem);
        }
    }
}
