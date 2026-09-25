package io.github.rutwoklabs.jbuildpilot.cli.pipeline;

import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.core.*;
import io.github.rutwoklabs.jbuildpilot.core.permission.*;
import io.github.rutwoklabs.jbuildpilot.resolver.*;
import io.github.rutwoklabs.jbuildpilot.environment.*;
import io.github.rutwoklabs.jbuildpilot.environment.detector.*;
import io.github.rutwoklabs.jbuildpilot.downloader.*;
import io.github.rutwoklabs.jbuildpilot.security.*;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.*;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AutoPipeline {
    private final ProjectAnalyzer analyzer;
    private final RequirementResolver resolver;
    private final EnvironmentDetector envDetector;
    private final PermissionManager permissionManager;
    private final Downloader downloader;
    private final VirtualSpace virtualSpace;
    private final JBuildPilotEngine engine;
    private final ArtifactRepository repository;

    public AutoPipeline(ProjectAnalyzer analyzer, 
                        RequirementResolver resolver, 
                        EnvironmentDetector envDetector, 
                        PermissionManager permissionManager, 
                        Downloader downloader, 
                        VirtualSpace virtualSpace, 
                        JBuildPilotEngine engine,
                        ArtifactRepository repository) {
        this.analyzer = analyzer;
        this.resolver = resolver;
        this.envDetector = envDetector;
        this.permissionManager = permissionManager;
        this.downloader = downloader;
        this.virtualSpace = virtualSpace;
        this.engine = engine;
        this.repository = repository;
    }

    private ToolInfo detectRequirement(Requirement req) {
        if (req.getType().name().equals("JAVA")) {
            return envDetector.detectJava(req.getVersionConstraint());
        } else if (req.getType().name().equals("MAVEN")) {
            return envDetector.detectMaven(req.getVersionConstraint());
        } else if (req.getType().name().equals("GRADLE")) {
            return envDetector.detectGradle(req.getVersionConstraint());
        }
        return new ToolInfo(req.getIdentity(), "0.0.0", null, RequirementStatus.UNKNOWN);
    }

    /**
     * Detect missing JAVA/MAVEN/GRADLE toolchains for the project and, with the user's
     * permission, download + verify + install them. Does NOT build the project.
     * Returns true if nothing was missing or every missing toolchain was installed.
     * This is the acquisition core shared by {@link #run} and by {@code pilot doctor --fix}.
     */
    public boolean acquire(Path projectDir) {
        // 1-4. Analyze project & detect dependencies
        ProjectModel model = analyzer.analyze(projectDir);

        // 5. Build requirement graph
        ResolutionResult resolution = resolver.resolve(model);

        // 6. Detect installed environment & 7. Identify missing
        List<Requirement> missing = new ArrayList<>();
        for (RequirementNode node : resolution.getGraph().getNodes()) {
            Requirement req = node.getRequirement();
            if (req == null) continue; // Root node
            ToolInfo info = detectRequirement(req);
            if (info.getStatus() == RequirementStatus.MISSING || info.getStatus() == RequirementStatus.UNKNOWN) {
                // We only download tools/java, not dependencies in this phase via Downloader (dependencies via build tool)
                if (req.getType().name().equals("JAVA") || req.getType().name().equals("MAVEN") || req.getType().name().equals("GRADLE")) {
                    missing.add(req);
                }
            }
        }

        // 8. Display missing
        if (missing.isEmpty()) {
            System.out.println("All required toolchains are already present. Nothing to acquire.");
            return true;
        }

        System.out.println("Missing requirements detected:");
        for (Requirement req : missing) {
            System.out.println("- " + req.getIdentity() + " @ " + req.getVersionConstraint().getRawConstraint());
        }

        // 9. Request user approval
        PermissionRequest preq = new PermissionRequest(PermissionType.DOWNLOAD_TOOL, "Download missing requirements: " + missing.size() + " items");
        PermissionDecision decision = permissionManager.requestPermission(preq);
        if (decision == PermissionDecision.DENY) {
            throw new SecurityException("Download permission denied.");
        }

        // 10. Acquire approved requirements
        for (Requirement req : missing) {
            // Resolve the concrete pinned distribution: versioned id + published checksum.
            // A null checksum means we have no pinned/verifiable entry for this tool, so we
            // refuse rather than download something we cannot integrity-check.
            Artifact artifact = repository.toArtifact(req.getIdentity(), req.getVersionConstraint());
            if (artifact.getExpectedChecksum() == null || artifact.getExpectedChecksum().trim().isEmpty()) {
                System.err.println("No pinned, checksum-verified distribution is available for '"
                        + req.getIdentity() + "'. Refusing to download an unverifiable artifact.");
                System.err.println("  Install " + req.getIdentity() + " manually, or add a pinned entry to PinnedToolRepository.");
                return false;
            }

            DownloadRequest dReq = new DownloadRequest(artifact, repository);
            DownloadResult dRes = downloader.download(dReq);
            if (!dRes.isSuccessful()) {
                System.err.println("Failed to download " + req.getIdentity() + ": " + dRes.getErrorMessage());
                return false;
            }

            // 11. Extract and Install resources
            try {
                ToolInstaller installer = new ToolInstaller(new PathManager(Paths.get(System.getProperty("user.home"), ".jbuildpilot")));
                installer.installTool(req.getIdentity(), dRes.getCachedPath());
                System.out.println("Installed " + req.getIdentity() + " successfully.");
            } catch (Exception e) {
                System.err.println("Failed to install " + req.getIdentity() + ": " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    public boolean run(Path projectDir, EngineContext context) {
        // 1-11. Resolve + acquire any missing toolchains (with permission).
        if (!acquire(projectDir)) {
            return false;
        }

        ProjectModel model = analyzer.analyze(projectDir);

        // 13. Validate environment (skipped in this mock pipeline as real tools aren't fully extracted)

        // 14. Build project
        PermissionDecision buildDec = permissionManager.requestPermission(new PermissionRequest(PermissionType.EXECUTE_BUILD, "Execute build for " + model.getMetadata().getName()));
        if (buildDec == PermissionDecision.DENY) {
            throw new SecurityException("Build execution denied.");
        }

        String buildSystem = model.getRequirements().stream()
            .filter(r -> r.getType().name().equals("MAVEN") || r.getType().name().equals("GRADLE"))
            .map(r -> r.getType().name().toLowerCase())
            .findFirst().orElse("maven");

        ExecutionPlan plan = new ExecutionPlan("AutoPipeline", List.of(new BuildStep(buildSystem)));

        try {
            engine.execute(plan, context);
            // 15. Report result
            System.out.println("Pipeline completed successfully.");
            return true;
        } catch (Exception e) {
            System.err.println("Pipeline failed: " + e.getMessage());
            return false;
        }
    }
}
