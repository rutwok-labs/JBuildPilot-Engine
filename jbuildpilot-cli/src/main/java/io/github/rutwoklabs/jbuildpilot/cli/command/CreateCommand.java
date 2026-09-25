package io.github.rutwoklabs.jbuildpilot.cli.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.template.*;
import io.github.rutwoklabs.jbuildpilot.template.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CreateCommand {
    private final String[] args;
    private final PermissionManager permissionManager;

    public CreateCommand(String[] args) {
        this.args = args;
        this.permissionManager = new PermissionManager(new PermissionSet(), new TerminalPermissionHandler());
    }

    public void execute() {
        TemplateRepository repo = new LocalTemplateRepository(Paths.get(io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.templatesDir()));
        TemplateRegistry registry = repo.fetchRegistry();
        TemplateResolver resolver = new TemplateResolver(registry);
        
        String templateId = null;
        String projectName = null;
        Scanner scanner = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.SCANNER;

        if (args.length >= 2) {
            templateId = args[1];
        }
        if (args.length >= 3) {
            projectName = args[2];
        }

        if (templateId == null) {
            System.out.println("What do you want to create?");
            List<TemplateInfo> all = registry.getTemplates();
            for (int i = 0; i < all.size(); i++) {
                System.out.println((i + 1) + ". " + all.get(i).getName());
            }
            System.out.print("Select: ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid selection: not a number.");
                return;
            }
            if (choice < 1 || choice > all.size()) {
                System.err.println("Invalid selection: out of range.");
                return;
            }
            templateId = all.get(choice - 1).getId();
        }

        // Ask permission before downloading
        PermissionRequest req = new PermissionRequest(PermissionType.TEMPLATE_DOWNLOAD, "download template: " + templateId);
        if (permissionManager.requestPermission(req) == PermissionDecision.DENY) {
            System.out.println("Template download denied. Aborting.");
            return;
        }

        System.out.println("Fetching template: " + templateId + "...");
        Path templateDir = repo.fetchTemplate(templateId);
        
        // Read template metadata
        Path metadataPath = templateDir.resolve("template.json");
        TemplateMetadata metadata;
        try {
            metadata = new ObjectMapper().readValue(metadataPath.toFile(), TemplateMetadata.class);
        } catch (Exception e) {
            System.err.println("Failed to read template.json: " + e.getMessage());
            return;
        }

        System.out.println("Template: " + metadata.getName());
        Map<String, String> variables = new HashMap<>();

        for (TemplateVariable var : metadata.getVariables()) {
            if ("PROJECT_NAME".equals(var.getName()) && projectName != null) {
                variables.put(var.getName(), projectName);
                continue;
            }
            System.out.print(var.getName() + ": > ");
            String val = scanner.nextLine().trim();
            // Basic validation
            if (val.contains("..") || val.contains("/") || val.contains("\\")) {
                throw new SecurityException("Invalid characters in variable input. Aborting to prevent path traversal.");
            }
            variables.put(var.getName(), val);
        }

        if (projectName == null) {
            projectName = variables.getOrDefault("PROJECT_NAME", "my-project");
        }
        
        Path targetDir = io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.workingDir().toAbsolutePath().resolve(projectName);
        System.out.println("Generating project in " + targetDir + "...");

        TemplateGenerator generator = new TemplateGenerator();
        generator.generate(templateDir, targetDir, variables);

        System.out.println("Project generated successfully!");
        
        // At this point we could call AutoPipeline to analyze and build,
        // but for now, generation is complete.
        // The user specifies: Pass the generated project to the normal analyzer.
        System.out.println("To build your new project, run: cd " + projectName + " && pilot build");
    }
}
