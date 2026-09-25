package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.resolver.RequirementResolver;
import io.github.rutwoklabs.jbuildpilot.resolver.ResolutionResult;
import io.github.rutwoklabs.jbuildpilot.resolver.RequirementNode;

import java.nio.file.Path;

public class ResolveCommand {
    public void execute() {
        System.out.println("Resolving Requirements...\n");
        
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        ProjectModel model = analyzer.analyze(io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir());
        
        RequirementResolver resolver = new RequirementResolver();
        ResolutionResult result = resolver.resolve(model);
        
        System.out.println("Requirement Graph:");
        for (RequirementNode node : result.getGraph().getNodes()) {
            String id = node.getRequirement() != null ? node.getRequirement().getIdentity() : node.getId();
            System.out.println("- " + id + " [" + node.getStatus() + "]");
        }
        
        System.out.println("\nMissing Requirements:");
        long missing = result.getGraph().getNodes().stream()
                .filter(n -> n.getStatus().name().equals("MISSING") || n.getStatus().name().equals("UNKNOWN"))
                .count();
                
        if (missing == 0) {
            System.out.println("None! Environment is satisfied.");
        } else {
            System.out.println(missing + " requirements are missing or unknown.");
        }
    }
}
