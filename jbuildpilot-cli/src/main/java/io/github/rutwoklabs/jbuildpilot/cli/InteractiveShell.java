package io.github.rutwoklabs.jbuildpilot.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Interactive, menu-driven front-end for the JBuildPilot CLI.
 *
 * <p>Launched when the CLI is started with the {@code interactive} command (this is
 * what the packaged {@code .exe} does on double-click) or with no arguments when a
 * real console is attached. It first asks the user for a working directory, then
 * presents a looping menu that dispatches to the existing {@link JBuildPilotCli}
 * commands. All operations still flow through the same zero-trust permission model.
 */
public final class InteractiveShell {

    private final Scanner scanner = JBuildPilotCli.SCANNER;

    /** Reads a line, returning {@code null} at end-of-input so callers can exit cleanly. */
    private String readLine() {
        try {
            return scanner.hasNextLine() ? scanner.nextLine() : null;
        } catch (java.util.NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    public int start() {
        banner();
        selectWorkingDirectory();
        return menuLoop();
    }

    private void banner() {
        // Pure-ASCII art: Windows consoles on the default code page render Unicode
        // box-drawing characters as '?', so we deliberately avoid them everywhere.
        System.out.println();
        System.out.println("    _ ____        _ _     _ ____  _ _       _   ");
        System.out.println("   | | __ )  _   _(_) | __| |  _ \\(_) | ___ | |_ ");
        System.out.println(" _ | |  _ \\ | | | | | |/ _` | |_) | | |/ _ \\| __|");
        System.out.println("| |_| | |_) || |_| | | | (_| |  __/| | | (_) | |_ ");
        System.out.println(" \\___/|____/  \\__,_|_|_|\\__,_|_|   |_|_|\\___/ \\__|");
        System.out.println();
        System.out.println("        J B U I L D P I L O T   E N G I N E");
        System.out.println("      Secure, deterministic Java build pilot");
        System.out.println("                  v0.1.0-alpha.1");
        System.out.println("==================================================");
    }

    /** Prompts (and re-prompts) for a valid project directory, then binds the CLI to it. */
    private void selectWorkingDirectory() {
        String current = Paths.get("").toAbsolutePath().toString();
        while (true) {
            System.out.println();
            System.out.println("Current directory: " + current);
            System.out.print("Enter your project working directory (blank = keep current): ");
            String input = scanner.nextLine().trim();
            // Strip a leading UTF-8 BOM that some shells inject on the first piped line.
            if (!input.isEmpty() && input.charAt(0) == '﻿') {
                input = input.substring(1).trim();
            }
            if (input.isEmpty()) {
                bindWorkingDirectory(current);
                return;
            }
            Path candidate;
            try {
                candidate = Paths.get(input).toAbsolutePath().normalize();
            } catch (java.nio.file.InvalidPathException e) {
                System.err.println("Invalid path: " + e.getMessage());
                continue;
            }
            if (!Files.isDirectory(candidate)) {
                System.err.println("Not a directory: " + candidate);
                continue;
            }
            bindWorkingDirectory(candidate.toString());
            return;
        }
    }

    /**
     * Binds subsequent commands to {@code dir} by setting {@link JBuildPilotCli}'s
     * working-directory override. Also sets {@code user.dir} for anything that reads
     * it directly. The OS process directory is left unchanged.
     */
    private void bindWorkingDirectory(String dir) {
        System.setProperty("user.dir", dir);
        JBuildPilotCli.setWorkingDir(Paths.get(dir));
        System.out.println("Working directory set to: " + dir);
    }

    private int menuLoop() {
        while (true) {
            printMenu();
            System.out.print("Select an option: ");
            String choice = readLine();
            if (choice == null) {
                System.out.println("Goodbye.");
                return 0;
            }
            choice = choice.trim().toLowerCase();
            switch (choice) {
                case "1" -> dispatch("analyze");
                case "2" -> dispatch("doctor");
                case "3" -> dispatch("doctor", "--fix");
                case "4" -> dispatch("resolve");
                case "5" -> dispatch("env");
                case "6" -> dispatch("build");
                case "7" -> dispatch("run");
                case "8" -> dispatch("project", "diagnose");
                case "9" -> dispatch("project", "apply");
                case "10" -> dispatch("template", "list");
                case "11" -> dispatch("create");
                case "12" -> customCommand();
                case "d" -> selectWorkingDirectory();
                case "0", "q", "exit", "quit" -> {
                    System.out.println("Goodbye.");
                    return 0;
                }
                default -> System.err.println("Unknown option: " + choice);
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("  Working dir: " + System.getProperty("user.dir"));
        System.out.println("==================================================");
        System.out.println("  PROJECT");
        System.out.println("     1) analyze            Inspect requirements");
        System.out.println("     8) project diagnose   Find outdated deps/Java");
        System.out.println("     9) project apply      Apply approved updates");
        System.out.println();
        System.out.println("  TOOLCHAINS");
        System.out.println("     2) doctor            Check host toolchains");
        System.out.println("     3) doctor --fix      Auto-install missing tools");
        System.out.println("     4) resolve           Compute what's needed");
        System.out.println("     5) env               Show Virtual Space paths");
        System.out.println();
        System.out.println("  BUILD / RUN");
        System.out.println("     6) build             Build the project (isolated)");
        System.out.println("     7) run               Run the compiled project");
        System.out.println();
        System.out.println("  SCAFFOLD");
        System.out.println("    10) template list     List available templates");
        System.out.println("    11) create            Generate a new project");
        System.out.println();
        System.out.println("    12) custom...         Type a raw command line");
        System.out.println("     d) change working directory");
        System.out.println("     0) exit");
        System.out.println("--------------------------------------------------");
    }

    private void customCommand() {
        System.out.print("Enter command (e.g. 'github issue 1' or 'run com.example.Main'): ");
        String line = readLine();
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        dispatch(line.trim().split("\\s+"));
    }

    /** Runs a CLI command in-process and reports a non-zero exit code without killing the shell. */
    private void dispatch(String... args) {
        System.out.println();
        int code;
        try {
            code = JBuildPilotCli.run(args);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            code = 2;
        }
        if (code != 0) {
            System.err.println("(command exited with code " + code + ")");
        }
        System.out.println();
        System.out.print("Press Enter to continue...");
        readLine();
    }
}
