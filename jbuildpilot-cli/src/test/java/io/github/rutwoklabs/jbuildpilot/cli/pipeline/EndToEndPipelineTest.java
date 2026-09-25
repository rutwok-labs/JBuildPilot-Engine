package io.github.rutwoklabs.jbuildpilot.cli.pipeline;

import com.sun.net.httpserver.HttpServer;
import io.github.rutwoklabs.jbuildpilot.analyzer.ProjectAnalyzer;
import io.github.rutwoklabs.jbuildpilot.cli.command.CliEngineContext;
import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import io.github.rutwoklabs.jbuildpilot.core.Requirement;
import io.github.rutwoklabs.jbuildpilot.core.RequirementStatus;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.downloader.ArtifactRepository;
import io.github.rutwoklabs.jbuildpilot.downloader.ChecksumVerifier;
import io.github.rutwoklabs.jbuildpilot.downloader.DownloadCache;
import io.github.rutwoklabs.jbuildpilot.downloader.Downloader;
import io.github.rutwoklabs.jbuildpilot.environment.PathManager;
import io.github.rutwoklabs.jbuildpilot.environment.VirtualSpace;
import io.github.rutwoklabs.jbuildpilot.environment.detector.EnvironmentDetector;
import io.github.rutwoklabs.jbuildpilot.environment.detector.ToolInfo;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.JBuildPilotEngine;
import io.github.rutwoklabs.jbuildpilot.resolver.RequirementResolver;
import io.github.rutwoklabs.jbuildpilot.security.MalwareScanner;
import io.github.rutwoklabs.jbuildpilot.security.ScanVerdict;
import io.github.rutwoklabs.jbuildpilot.security.SecurityPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndPipelineTest {

    @TempDir
    Path tempSpace;

    @TempDir
    Path projectDir;

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        TerminalPermissionHandler.MOCK_RESPONSE = "y";

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/artifact", exchange -> {
            byte[] response = "fake-tool-bytes".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        TerminalPermissionHandler.MOCK_RESPONSE = null;
    }

    @Test
    void testFullPipelineIntegration() throws Exception {
        // 1. Create a dummy Maven project
        Files.writeString(projectDir.resolve("pom.xml"), 
                "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>demo</artifactId><version>1</version></project>");

        // 2. Setup isolated Environment
        PathManager pathManager = new PathManager(tempSpace);
        VirtualSpace virtualSpace = new VirtualSpace(pathManager);
        virtualSpace.initialize();

        // 3. Setup Downloader with Security rules
        DownloadCache cache = new DownloadCache(virtualSpace);
        ChecksumVerifier verifier = new ChecksumVerifier() {
            @Override
            public boolean verify(Path file, String expectedChecksum) {
                return true; // Bypass checksum for E2E
            }
        };
        MalwareScanner scanner = file -> ScanVerdict.CLEAN;
        Downloader downloader = new Downloader(cache, verifier, scanner, SecurityPolicy.strict());
        // Repository serves the local artifact and advertises a (dummy) checksum so the
        // pipeline's "refuse unverifiable downloads" guard is satisfied; the overridden
        // verifier above bypasses the actual digest comparison for this E2E.
        ArtifactRepository repository = new ArtifactRepository() {
            @Override
            public URI resolveUri(io.github.rutwoklabs.jbuildpilot.downloader.Artifact a) {
                return URI.create("http://127.0.0.1:" + port + "/artifact");
            }

            @Override
            public String resolveChecksum(String identity, io.github.rutwoklabs.jbuildpilot.core.VersionConstraint vc) {
                return "0".repeat(64);
            }
        };

        // 4. Setup Custom Detector that reports MISSING so Downloader triggers
        io.github.rutwoklabs.jbuildpilot.environment.detector.JavaDetector javaDetector = req -> new ToolInfo("java", "0.0.0", null, RequirementStatus.MISSING);
        io.github.rutwoklabs.jbuildpilot.environment.detector.MavenDetector mavenDetector = req -> new ToolInfo("maven", "0.0.0", null, RequirementStatus.MISSING);
        io.github.rutwoklabs.jbuildpilot.environment.detector.GradleDetector gradleDetector = req -> new ToolInfo("gradle", "0.0.0", null, RequirementStatus.MISSING);
        
        EnvironmentDetector detector = new EnvironmentDetector(javaDetector, mavenDetector, gradleDetector);

        // 5. Setup permissions and engine
        PermissionManager pm = new PermissionManager(new PermissionSet(), new TerminalPermissionHandler());
        JBuildPilotEngine engine = new JBuildPilotEngine();
        CliEngineContext context = new CliEngineContext(pm) {
            @Override
            public Path getProjectDirectory() {
                return projectDir;
            }

            @Override
            public io.github.rutwoklabs.jbuildpilot.builder.BuildEngine getBuildEngine(String system) {
                // Stub the build engine so the pipeline's build step is deterministic and
                // does not depend on a real Maven/Gradle install existing in the isolated space.
                return request -> new io.github.rutwoklabs.jbuildpilot.builder.BuildResult(0, "BUILD SUCCESS", "");
            }
        };

        // 6. Build the pipeline
        AutoPipeline pipeline = new AutoPipeline(
                new ProjectAnalyzer(),
                new RequirementResolver(),
                detector,
                pm,
                downloader,
                virtualSpace,
                engine,
                repository
        );

        // 7. Execute!
        boolean success = pipeline.run(projectDir, context);
        
        // 8. Verify
        assertTrue(success, "End-to-End Pipeline should complete successfully.");
    }
}
