package io.github.rutwoklabs.jbuildpilot.cli.pipeline;

import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.downloader.ArtifactRepository;
import io.github.rutwoklabs.jbuildpilot.downloader.ChecksumVerifier;
import io.github.rutwoklabs.jbuildpilot.downloader.DownloadCache;
import io.github.rutwoklabs.jbuildpilot.downloader.Downloader;
import io.github.rutwoklabs.jbuildpilot.downloader.PinnedToolRepository;
import io.github.rutwoklabs.jbuildpilot.environment.PathManager;
import io.github.rutwoklabs.jbuildpilot.environment.VirtualSpace;
import io.github.rutwoklabs.jbuildpilot.environment.detector.EnvironmentDetector;
import io.github.rutwoklabs.jbuildpilot.environment.detector.HostGradleDetector;
import io.github.rutwoklabs.jbuildpilot.environment.detector.HostJavaDetector;
import io.github.rutwoklabs.jbuildpilot.environment.detector.HostMavenDetector;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.JBuildPilotEngine;
import io.github.rutwoklabs.jbuildpilot.resolver.RequirementResolver;
import io.github.rutwoklabs.jbuildpilot.security.ApiKeyProvider;
import io.github.rutwoklabs.jbuildpilot.security.MalwareScanner;
import io.github.rutwoklabs.jbuildpilot.security.MetaDefenderScanner;
import io.github.rutwoklabs.jbuildpilot.security.SecurityPolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single source of truth for building a fully-wired {@link AutoPipeline} plus its
 * {@link PermissionManager}, so {@code pilot build} and {@code pilot doctor --fix} share
 * identical security wiring instead of duplicating the recipe.
 *
 * <p>Security posture (per the project's chosen trust model):
 * <ul>
 *   <li>Distributions are pinned in-app with published checksums ({@link PinnedToolRepository}),
 *       so the verified checksum plus official-HTTPS-only sources are the trust anchor.</li>
 *   <li>Cloud malware scanning (MetaDefender) is optional. When no API key is configured we
 *       downgrade to {@code SecurityPolicy(false, true)} (skip-scan-on-failure) and print a
 *       loud warning; integrity is still enforced by the pinned checksum. When a key is present
 *       we use {@link SecurityPolicy#strict()} and a real cloud scan runs.</li>
 * </ul>
 */
public final class AutoPipelineFactory {

    /** An {@link AutoPipeline} together with the {@link PermissionManager} that drives it. */
    public record Wiring(AutoPipeline pipeline, PermissionManager permissionManager) { }

    private AutoPipelineFactory() { }

    /**
     * Builds the default pipeline rooted at {@code ~/.jbuildpilot}, using a terminal permission
     * prompt. Chooses the security policy based on MetaDefender key availability.
     *
     * @throws IOException if the virtual space cannot be initialized
     */
    public static Wiring create() throws IOException {
        PermissionManager pm = new PermissionManager(new PermissionSet(), new TerminalPermissionHandler());

        PathManager pathManager = new PathManager(Paths.get(System.getProperty("user.home"), ".jbuildpilot"));
        VirtualSpace virtualSpace = new VirtualSpace(pathManager);
        virtualSpace.initialize();

        DownloadCache cache = new DownloadCache(virtualSpace);
        ChecksumVerifier verifier = new ChecksumVerifier();
        ApiKeyProvider apiKeyProvider = new ApiKeyProvider(pathManager);
        MalwareScanner scanner = new MetaDefenderScanner(apiKeyProvider);

        SecurityPolicy policy = resolvePolicy(apiKeyProvider);
        Downloader downloader = new Downloader(cache, verifier, scanner, policy);

        // Pinned, checksum-verified distributions from official HTTPS sources.
        ArtifactRepository repository = new PinnedToolRepository();

        EnvironmentDetector detector = new EnvironmentDetector(
                new HostJavaDetector(),
                new HostMavenDetector(),
                new HostGradleDetector());

        AutoPipeline pipeline = new AutoPipeline(
                new ProjectAnalyzer(),
                new RequirementResolver(),
                detector,
                pm,
                downloader,
                virtualSpace,
                new JBuildPilotEngine(),
                repository);

        return new Wiring(pipeline, pm);
    }

    /**
     * Strict when a MetaDefender key is configured; otherwise skip-scan-on-failure with a loud
     * warning, relying on the pinned checksum + official HTTPS as the trust anchor.
     */
    private static SecurityPolicy resolvePolicy(ApiKeyProvider apiKeyProvider) {
        try {
            apiKeyProvider.getApiKey();
            return SecurityPolicy.strict();
        } catch (RuntimeException noKey) {
            System.out.println("WARNING: No MetaDefender API key configured; cloud malware scanning is DISABLED.");
            System.out.println("         Integrity is still enforced by pinned checksums over official HTTPS sources.");
            System.out.println("         To enable scanning, set JBUILDPILOT_METADEFENDER_APIKEY or config.properties.");
            return new SecurityPolicy(false, true);
        }
    }
}
