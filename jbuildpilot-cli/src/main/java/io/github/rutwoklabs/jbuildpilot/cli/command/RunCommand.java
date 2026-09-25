package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.JBuildPilotEngine;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.ExecutionPlan;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.RunStep;

import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public class RunCommand {
    private final String[] args;

    public RunCommand(String[] args) {
        this.args = args;
    }

    public void execute() {
        System.out.println("Initiating Run Sequence...");
        
        Path projectDir = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir().toAbsolutePath().normalize();
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        ProjectModel model = analyzer.analyze(projectDir);
        
        String mainClass = null;
        if (args.length > 1) {
            mainClass = args[1];
        } else {
            // Find main class from maven exec plugin or properties if available
            // As fallback
            mainClass = "Main";
        }
        
        String buildSystem = model.getRequirements().stream()
            .filter(r -> r.getType().name().equals("MAVEN") || r.getType().name().equals("GRADLE"))
            .map(r -> r.getType().name().toLowerCase())
            .findFirst().orElse("maven");

        PermissionManager pm = new PermissionManager(new PermissionSet(), new TerminalPermissionHandler());
        CliEngineContext context = new CliEngineContext(pm);
        
        ExecutionPlan plan = new ExecutionPlan("CLI-Run", Collections.singletonList(
                new RunStep(mainClass, buildSystem)
        ));
        
        JBuildPilotEngine engine = new JBuildPilotEngine();
        engine.execute(plan, context);
    }
}
