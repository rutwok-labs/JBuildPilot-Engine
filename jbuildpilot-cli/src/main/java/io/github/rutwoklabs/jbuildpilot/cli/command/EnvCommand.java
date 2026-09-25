package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;
import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;

import java.nio.file.Path;

public class EnvCommand {
    public void execute() {
        JBuildPilot pilot = JBuildPilot.builder()
                .project(io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir())
                .permissionHandler(new TerminalPermissionHandler())
                .build();
        
        pilot.environment();
    }
}
