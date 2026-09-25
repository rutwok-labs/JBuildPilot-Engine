package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

public class RequireDependencyStep implements ExecutionStep {
    private final String dependency;

    public RequireDependencyStep(String dependency) {
        this.dependency = dependency;
    }

    @Override
    public String getDescription() {
        return "Require Dependency: " + dependency;
    }

    @Override
    public void execute(EngineContext context) {
        // Checking for a dependency might involve downloading it to the cache if not present.
        PermissionDecision decision = context.getPermissionManager().requestPermission(
                new PermissionRequest(PermissionType.DOWNLOAD_TOOL, "Download dependency " + dependency)
        );
        
        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission denied to download dependency: " + dependency);
        }
        
        context.log("Verified Dependency requirement: " + dependency);
    }
}
