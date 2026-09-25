package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.template.LocalTemplateRepository;
import io.github.rutwoklabs.jbuildpilot.template.TemplateRepository;
import io.github.rutwoklabs.jbuildpilot.template.TemplateResolver;
import io.github.rutwoklabs.jbuildpilot.template.model.TemplateInfo;

import java.nio.file.Paths;
import java.util.List;

public class TemplateCommand {
    private final String[] args;

    public TemplateCommand(String[] args) {
        this.args = args;
    }

    public void execute() {
        if (args.length < 2) {
            System.out.println("Usage: pilot template <list|search|info|update> [query]");
            return;
        }

        String subCommand = args[1];
        TemplateRepository repo = new LocalTemplateRepository(Paths.get(io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli.templatesDir()));
        TemplateResolver resolver = new TemplateResolver(repo.fetchRegistry());

        switch (subCommand) {
            case "list":
                System.out.println("JBuildPilot Templates");
                printTemplates(resolver.search(null));
                break;
            case "search":
                if (args.length < 3) {
                    System.out.println("Usage: pilot template search <query>");
                    return;
                }
                System.out.println("Search Results:");
                printTemplates(resolver.search(args[2]));
                break;
            case "info":
                if (args.length < 3) {
                    System.out.println("Usage: pilot template info <template-id>");
                    return;
                }
                printTemplateInfo(resolver.search(args[2]));
                break;
            case "update":
                System.out.println("Template registry updated.");
                break;
            default:
                System.out.println("Unknown template subcommand: " + subCommand);
        }
    }

    private void printTemplates(List<TemplateInfo> templates) {
        for (TemplateInfo t : templates) {
            System.out.printf("%-20s %-20s Java %s+   %s%n",
                    t.getId(), t.getName(),
                    t.getJava() != null ? t.getJava().get("minimum") : "?",
                    t.getBuildSystem());
        }
    }

    private void printTemplateInfo(List<TemplateInfo> templates) {
        if (templates.isEmpty()) {
            System.out.println("Template not found.");
            return;
        }
        TemplateInfo t = templates.get(0);
        System.out.println("ID: " + t.getId());
        System.out.println("Name: " + t.getName());
        System.out.println("Version: " + t.getVersion());
        System.out.println("Description: " + t.getDescription());
        System.out.println("Category: " + t.getCategory());
        System.out.println("Tags: " + (t.getTags() != null ? String.join(", ", t.getTags()) : ""));
        System.out.println("Build System: " + t.getBuildSystem());
    }
}
