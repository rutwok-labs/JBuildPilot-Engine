package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import io.github.rutwoklabs.jbuildpilot.cli.pipeline.AutoPipeline;
import io.github.rutwoklabs.jbuildpilot.cli.pipeline.AutoPipelineFactory;
import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;

import java.nio.file.Path;

public class DoctorCommand {
    private final boolean fix;

    public DoctorCommand(boolean fix) {
        this.fix = fix;
    }

    public void execute() {
        Path projectDir = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir();

        // Always print the read-only host-toolchain report first.
        JBuildPilot pilot = JBuildPilot.builder()
                .project(projectDir)
                .permissionHandler(new TerminalPermissionHandler())
                .build();
        pilot.doctor(false);

        if (!fix) {
            return;
        }

        // --fix: actually acquire (download + verify + install) the missing toolchains
        // through the same secure pipeline that `pilot build` uses, but WITHOUT building.
        System.out.println("\n[Remediation] Attempting to acquire missing toolchains...");
        try {
            AutoPipeline pipeline = AutoPipelineFactory.create().pipeline();
            boolean ok = pipeline.acquire(projectDir.toAbsolutePath().normalize());
            if (ok) {
                System.out.println("Remediation complete. Re-run 'pilot doctor' to confirm.");
            } else {
                System.err.println("Remediation did not complete; see errors above.");
            }
        } catch (java.io.IOException e) {
            System.err.println("Remediation failed: could not initialize virtual space: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Remediation aborted: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Remediation failed: " + e.getMessage());
        }
    }
}
