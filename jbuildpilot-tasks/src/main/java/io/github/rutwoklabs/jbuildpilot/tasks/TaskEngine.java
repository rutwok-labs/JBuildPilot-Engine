package io.github.rutwoklabs.jbuildpilot.tasks;

import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.core.Requirement;
import io.github.rutwoklabs.jbuildpilot.core.RequirementType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class TaskEngine {
    
    private final ProjectAnalyzer analyzer;
    private int taskCounter = 1;

    public TaskEngine() {
        this.analyzer = new ProjectAnalyzer();
    }

    public MaintenancePlan diagnose(Path projectPath) {
        ProjectModel model = analyzer.analyze(projectPath);
        MaintenancePlan plan = new MaintenancePlan(model.getMetadata().getName());

        for (Requirement req : model.getRequirements()) {
            if (req.getType() == RequirementType.JAVA) {
                String javaVersion = req.getVersionConstraint().getRawConstraint();
                if (!"21".equals(javaVersion)) {
                    plan.addTask(new MaintenanceTask(
                            generateId(),
                            "ENVIRONMENT_UPDATE",
                            "java",
                            javaVersion,
                            "21",
                            RiskLevel.HIGH,
                            "Upgrade to current LTS version",
                            Collections.singletonList("pom.xml"),
                            true
                    ));
                }
            } else if (req.getType() == RequirementType.DEPENDENCY) {
                if (req.getIdentity().equals("org.junit.jupiter:junit-jupiter") && req.getVersionConstraint().getRawConstraint().startsWith("5.10")) {
                    plan.addTask(new MaintenanceTask(
                            generateId(),
                            "DEPENDENCY_UPDATE",
                            req.getIdentity(),
                            req.getVersionConstraint().getRawConstraint(),
                            "5.13.0",
                            RiskLevel.LOW,
                            "New compatible release available",
                            Collections.singletonList("pom.xml"),
                            true
                    ));
                }
                if (req.getIdentity().equals("com.google.guava:guava") && req.getVersionConstraint().getRawConstraint().startsWith("32")) {
                    plan.addTask(new MaintenanceTask(
                            generateId(),
                            "DEPENDENCY_UPDATE",
                            req.getIdentity(),
                            req.getVersionConstraint().getRawConstraint(),
                            "33.0.0-jre",
                            RiskLevel.MEDIUM,
                            "Security patch update",
                            Collections.singletonList("pom.xml"),
                            true
                    ));
                }
            }
        }

        return plan;
    }
    
    public void apply(MaintenancePlan plan, Path projectPath) throws IOException {
        List<MaintenanceTask> tasks = plan.getApprovedTasks();
        if (tasks.isEmpty()) {
            System.out.println("No approved tasks to apply.");
            return;
        }

        Path base = projectPath.normalize();
        for (MaintenanceTask task : tasks) {
            for (String file : task.getFiles()) {
                Path targetFile = base.resolve(file).normalize();
                if (!targetFile.startsWith(base)) {
                    throw new SecurityException("Path traversal attempt blocked for: " + file);
                }

                if (!Files.exists(targetFile)) {
                    System.err.println("File not found: " + targetFile);
                    continue;
                }

                // Create backup
                Path backupFile = targetFile.getParent().resolve(targetFile.getFileName().toString() + ".bak");
                Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Created backup: " + backupFile);

                // Read and modify
                String content = Files.readString(targetFile, StandardCharsets.UTF_8);
                boolean changed = false;

                if ("ENVIRONMENT_UPDATE".equals(task.getType()) && "java".equals(task.getTargetPackage())) {
                    if (content.contains("<maven.compiler.source>" + task.getCurrentVersion() + "</maven.compiler.source>")) {
                        content = content.replace("<maven.compiler.source>" + task.getCurrentVersion() + "</maven.compiler.source>",
                                "<maven.compiler.source>" + task.getTargetVersion() + "</maven.compiler.source>");
                        content = content.replace("<maven.compiler.target>" + task.getCurrentVersion() + "</maven.compiler.target>",
                                "<maven.compiler.target>" + task.getTargetVersion() + "</maven.compiler.target>");
                        changed = true;
                    }
                } else if ("DEPENDENCY_UPDATE".equals(task.getType())) {
                    String[] parts = task.getTargetPackage().split(":");
                    if (parts.length < 2) {
                        System.err.println("Skipping malformed dependency identity: " + task.getTargetPackage());
                        continue;
                    }
                    String artifactId = parts[1];
                    // Very simple XML text replacement for demo/MVP purpose, bound safely to the explicit tags
                    String search = "<artifactId>" + artifactId + "</artifactId>\\s*<version>" + task.getCurrentVersion() + "</version>";
                    String replace = "<artifactId>" + artifactId + "</artifactId>\n                <version>" + task.getTargetVersion() + "</version>";
                    String newContent = content.replaceAll(search, replace);
                    if (!newContent.equals(content)) {
                        content = newContent;
                        changed = true;
                    }
                }

                if (changed) {
                    Files.writeString(targetFile, content, StandardCharsets.UTF_8);
                    System.out.println("Successfully applied task " + task.getId() + " to " + targetFile.getFileName());
                } else {
                    System.out.println("No changes applied for task " + task.getId() + " on " + targetFile.getFileName() + " (pattern not found or already updated)");
                }
            }
        }
    }
    
    private String generateId() {
        return String.format("JB-%03d", taskCounter++);
    }
}
