package io.github.rutwoklabs.jbuildpilot.core.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionManagerTest {

    @Test
    void testAllowedPermissionDirectlyGranted() {
        PermissionSet preGranted = new PermissionSet();
        preGranted.grant(PermissionType.DOWNLOAD_TOOL);

        PermissionManager manager = new PermissionManager(preGranted, request -> PermissionDecision.DENY);

        PermissionRequest request = new PermissionRequest(PermissionType.DOWNLOAD_TOOL, "Download Maven 3.9");
        PermissionDecision decision = manager.requestPermission(request);

        assertEquals(PermissionDecision.APPROVE, decision, "Should allow if pre-granted in set");
    }

    @Test
    void testDeniedPermissionViaPrompt() {
        PermissionManager manager = new PermissionManager(new PermissionSet(), request -> PermissionDecision.DENY);

        PermissionRequest request = new PermissionRequest(PermissionType.EXECUTE_BUILD, "Run mvn clean install");
        PermissionDecision decision = manager.requestPermission(request);

        assertEquals(PermissionDecision.DENY, decision, "Should deny if prompt explicitly denies");
    }

    @Test
    void testDefaultAnalyzeAllowedWithoutPrompt() {
        // Even if no prompt is provided, ANALYZE should default to ALLOW
        PermissionManager manager = new PermissionManager(new PermissionSet(), null);

        // Request Analyze
        PermissionDecision analyzeDecision = manager.requestPermission(
                new PermissionRequest(PermissionType.ANALYZE_PROJECT, "Scan pom.xml")
        );
        assertEquals(PermissionDecision.APPROVE, analyzeDecision, "ANALYZE should default to APPROVE");

        // Request execution
        PermissionDecision executeDecision = manager.requestPermission(
                new PermissionRequest(PermissionType.EXECUTE_PROCESS, "Run arbitrary shell script")
        );
        assertEquals(PermissionDecision.DENY, executeDecision, "Dangerous ops must default to DENY");
    }

    @Test
    void testMultiplePermissionsAndMemoization() {
        PermissionSet preGranted = new PermissionSet();
        preGranted.grant(PermissionType.NETWORK_ACCESS);

        // Mock a smart prompt that only allows downloads
        PermissionHandler smartPrompt = req -> {
            if (req.type() == PermissionType.DOWNLOAD_TOOL) {
                return PermissionDecision.APPROVE;
            }
            return PermissionDecision.DENY;
        };

        PermissionManager manager = new PermissionManager(preGranted, smartPrompt);

        // Pre-granted: ALLOW
        assertEquals(PermissionDecision.APPROVE, 
                manager.requestPermission(new PermissionRequest(PermissionType.NETWORK_ACCESS, "Connect to repo")));

        // Prompt-approved: ALLOW
        assertEquals(PermissionDecision.APPROVE, 
                manager.requestPermission(new PermissionRequest(PermissionType.DOWNLOAD_TOOL, "Fetch library")));

        // Prompt-denied: DENY
        assertEquals(PermissionDecision.DENY, 
                manager.requestPermission(new PermissionRequest(PermissionType.EXECUTE_PROCESS, "Run main()")));

        // Verify the newly approved DOWNLOAD permission was saved to the set
        assertTrue(manager.getGrantedPermissions().isGranted(PermissionType.DOWNLOAD_TOOL), 
                "Prompt-approved permissions should be memoized in the PermissionSet");
    }
}
