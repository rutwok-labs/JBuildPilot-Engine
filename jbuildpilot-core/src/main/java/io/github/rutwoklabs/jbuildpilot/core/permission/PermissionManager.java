package io.github.rutwoklabs.jbuildpilot.core.permission;

public class PermissionManager {
    private final PermissionSet grantedPermissions;
    private final PermissionHandler handler;

    public PermissionManager(PermissionSet grantedPermissions, PermissionHandler handler) {
        this.grantedPermissions = grantedPermissions;
        this.handler = handler;
    }

    public PermissionDecision requestPermission(PermissionRequest request) {
        PermissionType type = request.type();

        if (grantedPermissions.isGranted(type)) {
            return PermissionDecision.APPROVE;
        }

        if (type == PermissionType.ANALYZE_PROJECT) {
            grantedPermissions.grant(type);
            return PermissionDecision.APPROVE;
        }

        if (handler != null) {
            PermissionDecision decision = handler.request(request);
            if (decision == PermissionDecision.APPROVE) {
                grantedPermissions.grant(type);
                return PermissionDecision.APPROVE;
            }
        }
        return PermissionDecision.DENY;
    }

    public PermissionSet getGrantedPermissions() {
        return grantedPermissions;
    }
}
