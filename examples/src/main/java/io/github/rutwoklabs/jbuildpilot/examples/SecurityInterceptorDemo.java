package io.github.rutwoklabs.jbuildpilot.examples;

import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionHandler;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Example 1: Security Interceptor Demo
 * 
 * Demonstrates how to embed JBuildPilot and pipe its zero-trust security requests
 * into a custom handler. In a real application, you would replace the print statements
 * with a JavaFX/Swing GUI dialog prompting the user for approval.
 */
public class SecurityInterceptorDemo {

    public static void main(String[] args) {
        System.out.println("--- JBuildPilot Security Interceptor Demo ---");

        // Use current directory for demonstration purposes
        Path projectPath = Paths.get(".").toAbsolutePath();

        // 1. Initialize the JBuildPilot API with a custom PermissionHandler
        JBuildPilot pilot = JBuildPilot.builder()
                .project(projectPath)
                .permissionHandler(new CustomGuiPermissionHandler())
                .defaultApprove(false) // Ensure zero-trust is active
                .build();

        try {
            System.out.println("\n[1/4] Analyzing project statically (No code execution)...");
            pilot.analyze();

            System.out.println("\n[2/4] Running Environment Doctor (Fix=true)...");
            // The doctor may trigger a DOWNLOAD_TOOL permission request if a JDK is missing
            pilot.doctor(true);

            System.out.println("\n[3/4] Resolving requirements graph...");
            pilot.resolve();

            System.out.println("\n[4/4] Executing Build Wrapper...");
            // The build will trigger EXECUTE_BUILD and NETWORK_CONNECT permissions
            pilot.build();

            System.out.println("\nBuild process completed successfully.");
        } catch (Exception e) {
            System.err.println("Pipeline failed: " + e.getMessage());
        }
    }

    /**
     * A mock GUI permission handler that intercepts engine requests.
     */
    static class CustomGuiPermissionHandler implements PermissionHandler {
        @Override
        public PermissionDecision request(PermissionRequest request) {
            System.out.println("\n==================================================");
            System.out.println("🛡️ [SECURITY INTERCEPT] JBuildPilot requires permission:");
            System.out.println("Action: " + request.type());
            System.out.println("Detail: " + request.description());
            System.out.println("Reason: " + request.reason());
            System.out.println("Admin Required: " + request.requiresAdministrator());
            System.out.println("==================================================");
            
            // For this automated demo, we auto-approve everything.
            // In reality, this is where you call: return showDialogToUser();
            System.out.println(">> Auto-Approving for Demo purposes.");
            return PermissionDecision.APPROVE;
        }
    }
}
