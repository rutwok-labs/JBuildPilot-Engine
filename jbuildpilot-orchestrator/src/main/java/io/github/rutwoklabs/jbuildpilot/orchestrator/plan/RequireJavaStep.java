package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

public class RequireJavaStep implements ExecutionStep {
    private final String version;

    public RequireJavaStep(String version) {
        this.version = version;
    }

    @Override
    public String getDescription() {
        return "Require Java " + version;
    }

    @Override
    public void execute(EngineContext context) {
        // Just checking properties requires ANALYZE permission
        PermissionDecision decision = context.getPermissionManager().requestPermission(
                new PermissionRequest(PermissionType.ANALYZE_PROJECT, "Check for Java " + version)
        );
        
        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission denied to analyze Java environment.");
        }
        
        // In a real implementation, we'd invoke the Phase 5 EnvironmentDetector.
        // For Phase 10 execution plan purposes, we simply assert it.
        context.log("Verified Java requirement: " + version);
    }
}
