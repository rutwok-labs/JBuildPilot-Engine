package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.tasks.MaintenancePlan;
import io.github.rutwoklabs.jbuildpilot.tasks.MaintenanceTask;
import io.github.rutwoklabs.jbuildpilot.tasks.TaskEngine;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectCommand {
    private final String[] args;

    public ProjectCommand(String[] args) {
        this.args = args;
    }

    public void execute() {
        if (args.length < 2) {
            System.err.println("Usage: pilot project <diagnose|plan|apply>");
            return;
        }

        String subCommand = args[1];
        TaskEngine engine = new TaskEngine();
        Path currentDir = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir().toAbsolutePath();

        if ("diagnose".equalsIgnoreCase(subCommand)) {
            System.out.println("JBuildPilot Project Diagnosis");
            System.out.println("--------------------------------");
            
            MaintenancePlan plan = engine.diagnose(currentDir);
            System.out.println("\nRepository: " + plan.getRepository() + "\n");
            
            System.out.println("TASKS");
            for (MaintenanceTask task : plan.getTasks()) {
                System.out.println(task.toString());
            }
        } else if ("apply".equalsIgnoreCase(subCommand)) {
            System.out.println("Applying tasks...");
            String targetTaskId = args.length > 2 ? args[2] : null;
            MaintenancePlan plan = engine.diagnose(currentDir);
            for (MaintenanceTask task : plan.getTasks()) {
                if (targetTaskId == null || targetTaskId.equals(task.getId())) {
                    task.setApproved(true);
                }
            }
            try {
                engine.apply(plan, currentDir);
                System.out.println("Project apply sequence completed.");
            } catch (java.io.IOException e) {
                System.err.println("Failed to apply tasks: " + e.getMessage());
            }
        } else {
            System.err.println("Unknown project command: " + subCommand);
        }
    }
}
