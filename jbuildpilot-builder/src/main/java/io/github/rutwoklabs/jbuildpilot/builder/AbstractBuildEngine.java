package io.github.rutwoklabs.jbuildpilot.builder;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;

import java.util.List;

public abstract class AbstractBuildEngine implements BuildEngine {
    
    private final PermissionManager permissionManager;
    private final ProcessRunner processRunner;

    protected AbstractBuildEngine(PermissionManager permissionManager, ProcessRunner processRunner) {
        this.permissionManager = permissionManager;
        this.processRunner = processRunner;
    }

    @Override
    public BuildResult execute(BuildRequest request) {
        // 1. Verify EXECUTE_BUILD permission
        PermissionDecision decision = permissionManager.requestPermission(
                new PermissionRequest(PermissionType.EXECUTE_BUILD, "Execute build in " + request.getProjectDirectory())
        );

        if (decision != PermissionDecision.APPROVE) {
            throw new SecurityException("Permission EXECUTE_BUILD was denied.");
        }

        // 2. Construct safe arguments
        List<String> command = buildCommand(request);

        // 3. Execute
        return processRunner.run(command, request.getProjectDirectory(), request.getJavaHome());
    }

    protected abstract List<String> buildCommand(BuildRequest request);

    protected boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
