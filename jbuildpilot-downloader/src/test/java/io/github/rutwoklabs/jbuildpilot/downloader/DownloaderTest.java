package io.github.rutwoklabs.jbuildpilot.downloader;

import com.sun.net.httpserver.HttpServer;
import io.github.rutwoklabs.jbuildpilot.environment.PathManager;
import io.github.rutwoklabs.jbuildpilot.environment.VirtualSpace;
import io.github.rutwoklabs.jbuildpilot.security.MalwareScanner;
import io.github.rutwoklabs.jbuildpilot.security.ScanVerdict;
import io.github.rutwoklabs.jbuildpilot.security.SecurityPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class DownloaderTest {

    @TempDir
    Path tempHome;

    private VirtualSpace virtualSpace;
    private DownloadCache cache;
    private ChecksumVerifier verifier;
    private Downloader downloader;
    private HttpServer server;
    private int port;
    private String correctChecksum;
    
    // Test control for malware scanner
    private ScanVerdict nextVerdict = ScanVerdict.CLEAN;
    private SecurityPolicy currentPolicy = SecurityPolicy.strict();

    @BeforeEach
    void setUp() throws Exception {
        PathManager pathManager = new PathManager(tempHome);
        virtualSpace = new VirtualSpace(pathManager);
        virtualSpace.initialize();

        cache = new DownloadCache(virtualSpace);
        verifier = new ChecksumVerifier();
        
        MalwareScanner mockScanner = file -> nextVerdict;
        downloader = new Downloader(cache, verifier, mockScanner, currentPolicy);

        // Setup local HTTP server for testing without public internet
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        
        String dummyContent = "dummy-artifact-bytes";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(dummyContent.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        correctChecksum = sb.toString();

        server.createContext("/artifact", exchange -> {
            byte[] response = dummyContent.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        
        server.createContext("/fail", exchange -> {
            exchange.sendResponseHeaders(500, -1); // Simulates server error for retries
        });
        
        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testSuccessfulDownloadWithChecksum() {
        Artifact artifact = new Artifact("test-lib-1.0", correctChecksum);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertTrue(result.isSuccessful(), "Download should succeed");
        assertTrue(Files.exists(result.getCachedPath()), "Cached file must exist");
        assertTrue(result.getCachedPath().toString().contains("test-lib-1_0.bin"), "Filename should be sanitized");
    }

    @Test
    void testFailedChecksum() {
        Artifact artifact = new Artifact("test-lib-bad", "badchecksum0000");
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Checksum validation failed"));
    }

    @Test
    void testInsecureHttpBlocked() {
        Artifact artifact = new Artifact("test-insecure", null);
        // Using a non-localhost, non-https URI to trigger security block
        ArtifactRepository repo = a -> URI.create("http://example.com/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Insecure HTTP protocol is strictly forbidden"));
    }

    @Test
    void testPathTraversalMitigation() {
        // Artifact ID attempts to traverse out of the cache
        Artifact malicious = new Artifact("../../etc/passwd", null);
        assertEquals("______etc_passwd.bin", malicious.getSafeFileName(), "Traversal should be sanitized to underscores");
    }

    @Test
    void testMaliciousDownloadThrowsException() {
        nextVerdict = ScanVerdict.MALICIOUS;
        Artifact artifact = new Artifact("test-malicious", correctChecksum);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("MALICIOUS"));
    }

    @Test
    void testSuspiciousDownloadThrowsExceptionByDefault() {
        nextVerdict = ScanVerdict.SUSPICIOUS;
        Artifact artifact = new Artifact("test-suspicious", correctChecksum);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("SUSPICIOUS"));
    }

    @Test
    void testUnknownScanFailsByDefault() {
        nextVerdict = ScanVerdict.UNKNOWN;
        Artifact artifact = new Artifact("test-unknown", correctChecksum);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Malware scan failed or returned UNKNOWN"));
    }

    @Test
    void testNullChecksumRejectedByDefault() {
        // Task 2: Verify that null expectedChecksum fails verification by default
        Artifact artifact = new Artifact("test-null-checksum", null);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/artifact");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Checksum validation failed"), "Null checksum should trigger a validation failure");
    }

    @Test
    void testRedirectToInsecureHttpIsBlocked() {
        // Task 4: Setup a redirect from localhost to a non-HTTPS external URI
        server.createContext("/redirect-to-http", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://example.com/artifact");
            exchange.sendResponseHeaders(302, -1);
        });

        Artifact artifact = new Artifact("test-redirect", correctChecksum);
        ArtifactRepository repo = a -> URI.create("http://127.0.0.1:" + port + "/redirect-to-http");
        
        DownloadRequest request = new DownloadRequest(artifact, repo);
        DownloadResult result = downloader.download(request);
        
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Insecure HTTP protocol"), "Should block redirect to HTTP scheme on an external host");
    }
}