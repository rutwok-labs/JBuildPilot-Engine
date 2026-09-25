package io.github.rutwoklabs.jbuildpilot.cli;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionHandler;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;

public class TerminalPermissionHandler implements PermissionHandler {

    public static String MOCK_RESPONSE = null;

    @Override
    public PermissionDecision request(PermissionRequest request) {
        System.out.println("\nJBuildPilot Permission Request");
        System.out.println("--------------------------------");
        System.out.println("Action:");
        System.out.println("  " + request.description());

        if (request.toolName() != null) {
            System.out.println("\nTool:");
            System.out.println("  " + request.toolName());
            if (request.toolVersion() != null) {
                System.out.println("Version:\n  " + request.toolVersion());
            }
        }

        if (request.targetPath() != null) {
            System.out.println("\nTarget:\n  " + request.targetPath());
        }

        if (request.reason() != null) {
            System.out.println("\nReason:\n  " + request.reason());
        }

        if (request.downloadSize() > 0) {
            System.out.println("\nDownload:\n  ~" + (request.downloadSize() / 1024 / 1024) + " MB");
        }

        System.out.println("\nAdministrator privileges:\n  " + (request.requiresAdministrator() ? "Yes" : "No"));
        
        System.out.print("\nAllow this operation? [y/N]: ");
        String response;
        if (MOCK_RESPONSE != null) {
            response = MOCK_RESPONSE;
            System.out.println(response);
        } else {
            response = JBuildPilotCli.SCANNER.nextLine().trim().toLowerCase();
        }

        if (response.equals("y") || response.equals("yes")) {
            return PermissionDecision.APPROVE;
        }

        return PermissionDecision.DENY;
    }
}
