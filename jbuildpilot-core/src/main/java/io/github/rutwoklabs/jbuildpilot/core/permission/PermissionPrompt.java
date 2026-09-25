package io.github.rutwoklabs.jbuildpilot.core.permission;

/**
 * Strategy interface for resolving permissions interactively or programmatically
 * when a permission has not been pre-granted.
 */
public interface PermissionPrompt {
    PermissionDecision ask(PermissionRequest request);
}
