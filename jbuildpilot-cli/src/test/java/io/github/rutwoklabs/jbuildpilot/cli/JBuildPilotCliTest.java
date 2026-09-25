package io.github.rutwoklabs.jbuildpilot.cli;

import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JBuildPilotCliTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        TerminalPermissionHandler.MOCK_RESPONSE = "y"; // Auto-approve permissions for tests
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        TerminalPermissionHandler.MOCK_RESPONSE = null;
    }

    @Test
    void testAnalyzeCommand() {
        int exitCode = JBuildPilotCli.run(new String[]{"analyze"});
        assertEquals(0, exitCode);
        
        String output = outContent.toString();
        assertTrue(output.contains("JBUILDPILOT PROJECT ANALYSIS"), "Expected rich analysis header");
        assertTrue(output.contains("[Metadata]"), "Expected Metadata section");
    }

    @Test
    void testDoctorCommand() {
        int exitCode = JBuildPilotCli.run(new String[]{"doctor"});
        assertEquals(0, exitCode);
        
        String output = outContent.toString();
        assertTrue(output.contains("JBuildPilot Doctor"));
    }

    @Test
    void testResolveCommand() {
        int exitCode = JBuildPilotCli.run(new String[]{"resolve"});
        if (exitCode != 0) {
            originalErr.println("Resolve failed with: " + errContent.toString());
        }
        assertEquals(0, exitCode);
        
        String output = outContent.toString();
        assertTrue(output.contains("Resolving Requirements..."));
    }

    @Test
    void testBuildCommandWithMockPermissionDenied() {
        TerminalPermissionHandler.MOCK_RESPONSE = "n"; // Deny execution
        
        int exitCode = JBuildPilotCli.run(new String[]{"build"});
        assertEquals(3, exitCode); // Security exception code
        
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("[SECURITY] Operation aborted"));
    }
}
