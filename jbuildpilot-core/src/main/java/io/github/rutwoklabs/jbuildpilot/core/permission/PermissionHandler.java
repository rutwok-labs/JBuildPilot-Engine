package io.github.rutwoklabs.jbuildpilot.core.permission;

public interface PermissionHandler {
    PermissionDecision request(PermissionRequest request);
}
