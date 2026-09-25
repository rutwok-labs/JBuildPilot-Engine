package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.cli.pipeline.AutoPipeline;
import io.github.rutwoklabs.jbuildpilot.cli.pipeline.AutoPipelineFactory;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;

import java.nio.file.Path;

public class BuildCommand {

    private final Path projectDir;

    public BuildCommand() {
        this(io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir().toAbsolutePath().normalize());
    }

    public BuildCommand(Path projectDir) {
        this.projectDir = projectDir;
    }

    public void execute() {
        System.out.println("Initiating Autonomous Build Sequence...");

        // Shared, security-hardened wiring (pinned checksums, MetaDefender-optional posture).
        AutoPipelineFactory.Wiring wiring;
        try {
            wiring = AutoPipelineFactory.create();
        } catch (java.io.IOException e) {
            System.err.println("Failed to initialize virtual space: " + e.getMessage());
            return;
        }

        PermissionManager pm = wiring.permissionManager();
        AutoPipeline pipeline = wiring.pipeline();

        CliEngineContext context = new CliEngineContext(pm) {
            @Override
            public Path getProjectDirectory() {
                return projectDir;
            }
        };

        // Run the pipeline
        boolean success = pipeline.run(projectDir, context);
        if (success) {
            System.out.println("Build Sequence Completed Successfully.");
        } else {
            throw new RuntimeException("Build Sequence Failed.");
        }
    }
}
