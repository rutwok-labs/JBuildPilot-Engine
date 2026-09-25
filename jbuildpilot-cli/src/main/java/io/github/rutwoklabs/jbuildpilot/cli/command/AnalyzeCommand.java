package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.analyzer.BuildSystemDetector;
import io.github.rutwoklabs.jbuildpilot.analyzer.BuildSystemType;
import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.core.Requirement;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyzeCommand {
    public void execute() {
        Path dir = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir();

        // Honesty guard: with no build file there is nothing real to analyze. Rather than
        // print a hollow project derived from the folder name, say so plainly.
        if (new BuildSystemDetector().detect(dir) == BuildSystemType.UNKNOWN) {
            System.out.println("==================================================");
            System.out.println(" JBUILDPILOT PROJECT ANALYSIS");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("No Maven or Gradle project found in:");
            System.out.println("  " + dir.toAbsolutePath().normalize());
            System.out.println();
            System.out.println("Looked for: pom.xml, build.gradle(.kts), settings.gradle(.kts)");
            System.out.println("Tip: choose your project directory ('d' in the menu), or scaffold");
            System.out.println("     a new one with 'create' (option 11).");
            System.out.println("==================================================");
            return;
        }

        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        ProjectModel model = analyzer.analyze(dir);
        
        System.out.println("==================================================");
        System.out.println(" JBUILDPILOT PROJECT ANALYSIS");
        System.out.println("==================================================");
        
        System.out.println("\n[Metadata]");
        System.out.println("  Name:        " + model.getMetadata().getName());
        if (model.getMetadata().getGroupId() != null) {
            System.out.println("  Group:       " + model.getMetadata().getGroupId());
        }
        if (model.getMetadata().getVersion() != null) {
            System.out.println("  Version:     " + model.getMetadata().getVersion());
        }
        if (model.getMetadata().getDescription() != null) {
            System.out.println("  Details:     " + model.getMetadata().getDescription());
        }
        
        String buildSystem = "Unknown";
        String javaVersion = "Unknown";
        
        List<Requirement> deps = model.getRequirements().stream()
            .filter(r -> r.getType().name().equals("DEPENDENCY"))
            .collect(Collectors.toList());
            
        List<Requirement> plugins = model.getRequirements().stream()
            .filter(r -> r.getType().name().equals("PLUGIN"))
            .collect(Collectors.toList());
        
        for (Requirement req : model.getRequirements()) {
            if (req.getType().name().equals("MAVEN")) buildSystem = "Maven";
            if (req.getType().name().equals("GRADLE")) buildSystem = "Gradle";
            if (req.getType().name().equals("JAVA")) javaVersion = req.getVersionConstraint().getRawConstraint();
        }
        
        System.out.println("\n[Environment Requirements]");
        System.out.println("  Build Tool:  " + buildSystem);
        System.out.println("  Java Target: " + javaVersion);
        
        System.out.println("\n[Dependencies (" + deps.size() + ")]");
        if (deps.isEmpty()) {
            System.out.println("  (None detected)");
        } else {
            for (Requirement req : deps) {
                System.out.println("  - " + req.getIdentity() + " @ " + req.getVersionConstraint().getRawConstraint());
            }
        }

        if (!plugins.isEmpty()) {
            System.out.println("\n[Plugins (" + plugins.size() + ")]");
            for (Requirement req : plugins) {
                System.out.println("  - " + req.getIdentity() + " @ " + req.getVersionConstraint().getRawConstraint());
            }
        }
        
        System.out.println("\n==================================================");
    }
}
