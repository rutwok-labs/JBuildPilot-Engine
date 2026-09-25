package io.github.rutwoklabs.jbuildpilot.examples;

import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Example 2: Auto Builder Demo
 * 
 * Demonstrates how to use JBuildPilot in an automated CI/CD pipeline environment.
 * The defaultApprove(true) flag overrides the zero-trust system to allow headless builds.
 */
public class AutoBuilderDemo {

    public static void main(String[] args) {
        System.out.println("--- JBuildPilot Automated CI/CD Demo ---");

        Path targetRepo = Paths.get(".").toAbsolutePath();

        // 1. Initialize API in Headless / Auto-Approve mode
        JBuildPilot pilot = JBuildPilot.builder()
                .project(targetRepo)
                .defaultApprove(true) // WARNING: Bypasses interactive security prompts!
                .nonInteractive(true)
                .build();

        try {
            System.out.println("Executing automated build pipeline...");
            pilot.analyze();
            
            // Auto-fix missing toolchains silently
            pilot.doctor(true);
            
            pilot.resolve();
            pilot.build();

            System.out.println("Pipeline SUCCESS.");
        } catch (Exception e) {
            System.err.println("Pipeline FAILED: " + e.getMessage());
            System.exit(1);
        }
    }
}
