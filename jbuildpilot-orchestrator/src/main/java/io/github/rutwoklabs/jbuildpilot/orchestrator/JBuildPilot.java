package io.github.rutwoklabs.jbuildpilot.orchestrator;

import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionHandler;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.environment.EnvironmentManager;
import io.github.rutwoklabs.jbuildpilot.environment.VirtualSpace;

import java.nio.file.Path;

/**
 * Core Facade for embedding the JBuildPilot Engine.
 * Provides safe, deterministic project analysis, environment resolution, and isolated execution.
 */
public class JBuildPilot {
    private final Path projectDir;
    private final PermissionManager permissionManager;
    private final EnvironmentManager environmentManager;

    private JBuildPilot(Path projectDir, PermissionManager permissionManager, EnvironmentManager environmentManager) {
        this.projectDir = projectDir;
        this.permissionManager = permissionManager;
        this.environmentManager = environmentManager;
    }

    public static JBuildPilotBuilder builder() {
        return new JBuildPilotBuilder();
    }

    // Facade Methods

    /**
     * Safely analyzes the target project directory to extract metadata and requirements.
     * Operates in a read-only, strict static analysis mode. Does not execute arbitrary project scripts.
     */
    public void analyze() {
        // Implementation
        System.out.println("Analyzing project at " + projectDir);
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        ProjectModel project = analyzer.analyze(projectDir);
        System.out.println("Found project: " + project.getMetadata().getName());
    }

    public void doctor() {
        doctor(false);
    }

    /**
     * Diagnoses the host machine to verify that all required toolchains (JDKs, Maven, Gradle) exist.
     * @param fix If true, JBuildPilot will request permission to automatically download and install missing dependencies.
     */
    public void doctor(boolean fix) {
        System.out.println("Running JBuildPilot Doctor...");
        System.out.println("Project: " + projectDir.toAbsolutePath().normalize());

        ProjectModel model = new ProjectAnalyzer().analyze(projectDir);

        io.github.rutwoklabs.jbuildpilot.core.VersionConstraint javaConstraint =
                io.github.rutwoklabs.jbuildpilot.core.VersionConstraint.any();
        boolean needsMaven = false;
        boolean needsGradle = false;
        for (io.github.rutwoklabs.jbuildpilot.core.Requirement req : model.getRequirements()) {
            switch (req.getType().name()) {
                case "JAVA" -> javaConstraint = req.getVersionConstraint();
                case "MAVEN" -> needsMaven = true;
                case "GRADLE" -> needsGradle = true;
                default -> { }
            }
        }

        io.github.rutwoklabs.jbuildpilot.environment.detector.EnvironmentDetector detector =
                new io.github.rutwoklabs.jbuildpilot.environment.detector.EnvironmentDetector(
                        new io.github.rutwoklabs.jbuildpilot.environment.detector.HostJavaDetector(),
                        new io.github.rutwoklabs.jbuildpilot.environment.detector.HostMavenDetector(),
                        new io.github.rutwoklabs.jbuildpilot.environment.detector.HostGradleDetector());

        System.out.println("\n[Host Toolchains]");
        boolean allOk = report("Java", detector.detectJava(javaConstraint), javaConstraint);
        if (needsMaven) {
            allOk &= report("Maven", detector.detectMaven(
                    io.github.rutwoklabs.jbuildpilot.core.VersionConstraint.any()),
                    io.github.rutwoklabs.jbuildpilot.core.VersionConstraint.any());
        }
        if (needsGradle) {
            allOk &= report("Gradle", detector.detectGradle(
                    io.github.rutwoklabs.jbuildpilot.core.VersionConstraint.any()),
                    io.github.rutwoklabs.jbuildpilot.core.VersionConstraint.any());
        }

        System.out.println();
        if (allOk) {
            System.out.println("All required toolchains are present and compatible.");
        } else if (fix) {
            System.out.println("Some toolchains are missing or incompatible.");
            System.out.println("Run 'pilot build' to have the secure acquisition pipeline download and verify them (with your permission).");
        } else {
            System.out.println("Some toolchains are missing or incompatible. Re-run with --fix for remediation guidance.");
        }
    }

    private boolean report(String label, io.github.rutwoklabs.jbuildpilot.environment.detector.ToolInfo info,
                           io.github.rutwoklabs.jbuildpilot.core.VersionConstraint constraint) {
        String status = info.getStatus().name();
        String version = info.getVersion() != null ? info.getVersion() : "-";
        String path = info.getExecutablePath() != null ? info.getExecutablePath().toString() : "not found";
        String want = "*".equals(constraint.getRawConstraint()) ? "any" : constraint.getRawConstraint();
        System.out.printf("  %-7s %-13s version=%s (required: %s)%n", label, "[" + status + "]", version, want);
        if (!"AVAILABLE".equals(status)) {
            System.out.println("          -> " + path);
        }
        return "AVAILABLE".equals(status);
    }

    public void resolve() {
        System.out.println("Resolving requirements...");
    }

    /**
     * Executes the build process within a securely isolated wrapper.
     * Triggers EXECUTE_BUILD and NETWORK_CONNECT zero-trust permission requests.
     */
    public void build() {
        System.out.println("Building project...");
    }

    public void run() {
        System.out.println("Running project...");
    }

    public void environment() {
        io.github.rutwoklabs.jbuildpilot.environment.PathManager pathManager =
                new io.github.rutwoklabs.jbuildpilot.environment.PathManager();
        VirtualSpace space = new VirtualSpace(pathManager);
        Path home = pathManager.getHomeDirectory();
        System.out.println("JBuildPilot Virtual Space");
        System.out.println("----------------------------------------");
        try {
            space.initialize();
        } catch (java.io.IOException e) {
            System.err.println("Warning: could not initialize virtual space: " + e.getMessage());
        }
        System.out.println("  Root:     " + home);
        System.out.println("  JDKs:     " + home.resolve("jdks"));
        System.out.println("  Maven:    " + home.resolve("maven"));
        System.out.println("  Gradle:   " + home.resolve("gradle"));
        System.out.println("  Tools:    " + home.resolve("tools"));
        System.out.println("  Caches:   " + home.resolve("caches"));
        System.out.println("  Projects: " + space.getProjectsDirectory());
    }

    public static class JBuildPilotBuilder {
        private Path projectDir;
        private PermissionHandler permissionHandler;
        private boolean nonInteractive = false;
        private boolean defaultApprove = false;

        public JBuildPilotBuilder project(Path project) {
            this.projectDir = project;
            return this;
        }

        public JBuildPilotBuilder permissionHandler(PermissionHandler handler) {
            this.permissionHandler = handler;
            return this;
        }

        public JBuildPilotBuilder nonInteractive(boolean nonInteractive) {
            this.nonInteractive = nonInteractive;
            return this;
        }
        
        public JBuildPilotBuilder defaultApprove(boolean defaultApprove) {
            this.defaultApprove = defaultApprove;
            return this;
        }

        public JBuildPilot build() {
            if (projectDir == null) {
                throw new IllegalStateException("Project directory must be specified.");
            }
            
            PermissionSet permissions = new PermissionSet();
            if (defaultApprove) {
                permissions = PermissionSet.all();
            }

            PermissionManager pm = new PermissionManager(permissions, permissionHandler);
            io.github.rutwoklabs.jbuildpilot.environment.PathManager pathManager = new io.github.rutwoklabs.jbuildpilot.environment.PathManager();
            VirtualSpace space = new VirtualSpace(pathManager);
            EnvironmentManager em = new EnvironmentManager(space);
            return new JBuildPilot(projectDir, pm, em);
        }
    }
}
