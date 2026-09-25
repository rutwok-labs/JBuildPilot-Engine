package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

public class RequireToolStep implements ExecutionStep {
    private final String tool;

    public RequireToolStep(String tool) {
        this.tool = tool;
    }

    @Override
    public String getDescription() {
        return "Require Tool: " + tool;
    }

    @Override
    public void execute(EngineContext context) {
        PermissionDecision decision = context.getPermissionManager().requestPermission(
                new PermissionRequest(PermissionType.ANALYZE_PROJECT, "Check for tool " + tool)
        );
        
        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission denied to analyze Tool environment.");
        }
        
        context.log("Verified Tool requirement: " + tool);
    }
}
