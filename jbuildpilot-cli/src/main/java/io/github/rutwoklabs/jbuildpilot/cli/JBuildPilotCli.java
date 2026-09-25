package io.github.rutwoklabs.jbuildpilot.cli;

import io.github.rutwoklabs.jbuildpilot.cli.command.AnalyzeCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.BuildCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.CreateCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.DoctorCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.EnvCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.ProjectCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.ResolveCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.RunCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.TemplateCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.ToolCommand;
import io.github.rutwoklabs.jbuildpilot.cli.command.GithubCommand;

import java.nio.file.Path;
import java.util.Scanner;

public class JBuildPilotCli {
    public static final Scanner SCANNER = new Scanner(System.in);

    /**
     * The directory all commands operate on. Defaults to the process launch
     * directory ("."). The interactive shell overrides it via {@link #setWorkingDir}
     * so the user can pick a project without changing the OS process directory
     * (the {@code user.dir} property is cached by the platform FileSystem on some
     * JDKs and cannot be relied on to redirect {@code Path.of(".")}).
     */
    private static volatile Path workingDir = Path.of(".");

    public static Path workingDir() {
        return workingDir;
    }

    public static void setWorkingDir(Path dir) {
        workingDir = dir;
    }

    /**
     * Resolves the local template repository directory. Overridable via the
     * {@code jbuildpilot.templates.dir} system property or the
     * {@code JBUILDPILOT_TEMPLATES_DIR} environment variable so the CLI is not
     * bound to a single machine's absolute path.
     */
    public static String templatesDir() {
        String prop = System.getProperty("jbuildpilot.templates.dir");
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        String env = System.getenv("JBUILDPILOT_TEMPLATES_DIR");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "G:/tools/JavaTools/jbuildpilot-templates";
    }

    public static void main(String[] args) {
        // Double-clicking the packaged .exe launches with no args and a real console:
        // drop the user straight into the interactive shell instead of printing help.
        if (args.length == 0 && System.console() != null) {
            System.exit(new InteractiveShell().start());
        }
        System.exit(run(args));
    }

    public static int run(String[] args) {
        if (args.length == 0) {
            printHelp();
            return 1;
        }
        if ("interactive".equals(args[0]) || "menu".equals(args[0]) || "shell".equals(args[0])) {
            return new InteractiveShell().start();
        }
        if ("--help".equals(args[0]) || "-h".equals(args[0]) || "help".equals(args[0])) {
            printHelp();
            return 0;
        }
        
        if ("--version".equals(args[0]) || "-v".equals(args[0])) {
            System.out.println("JBuildPilot Engine CLI v0.1.0-alpha.1");
            return 0;
        }

        String command = args[0];
        try {
            switch (command) {
                case "analyze" -> new AnalyzeCommand().execute();
                case "doctor" -> {
                    boolean fix = args.length > 1 && args[1].equals("--fix");
                    new DoctorCommand(fix).execute();
                }
                case "resolve" -> new ResolveCommand().execute();
                case "build" -> new BuildCommand().execute();
                case "run" -> new RunCommand(args).execute();
                case "project" -> new ProjectCommand(args).execute();
                case "tool" -> {
                    String action = args.length > 1 ? args[1] : null;
                    String tool = args.length > 2 ? args[2] : null;
                    String hash = null;
                    for (int i = 0; i < args.length; i++) {
                        if ("--verify-sha256".equals(args[i]) && i + 1 < args.length) {
                            hash = args[i + 1];
                            break;
                        }
                    }
                    new ToolCommand(action, tool, hash).execute();
                }
                case "help" -> printHelp();
                case "env" -> new EnvCommand().execute();
                case "template" -> new TemplateCommand(args).execute();
                case "create" -> new CreateCommand(args).execute();
                case "github" -> new GithubCommand(args).execute();
                default -> {
                    System.err.println("Unknown command: " + command);
                    printHelp();
                    return 1;
                }
            }
            return 0;
        } catch (SecurityException e) {
            System.err.println("\n[SECURITY] Operation aborted: " + e.getMessage());
            return 3;
        } catch (Exception e) {
            System.err.println("\n[ERROR] Command failed: " + e.getMessage());
            if ("true".equals(System.getenv("DEBUG"))) {
                e.printStackTrace();
            } else {
                System.err.println("Run with DEBUG=true for full stack trace.");
            }
            return 2;
        }
    }

    private static void printHelp() {
        System.out.println("JBuildPilot CLI - Usage:");
        System.out.println("  pilot interactive - Launch the guided menu (default on double-click)");
        System.out.println("  pilot analyze  - Analyze project structure and requirements");
        System.out.println("  pilot doctor   - Check environment health and toolchains");
        System.out.println("  pilot resolve  - Compute missing requirements");
        System.out.println("  pilot build    - Build the current project");
        System.out.println("  pilot run      - Run the current project");
        System.out.println("  pilot env      - Display Virtual Space paths");
        System.out.println("  pilot template - Manage templates (list, search, info)");
        System.out.println("  pilot create   - Create a new project from a template");
        System.out.println("  pilot github   - Invoke embedded GitHub Action integration");
    }
}
