package io.github.rutwoklabs.jbuildpilot.security;

import com.sun.net.httpserver.HttpServer;
import io.github.rutwoklabs.jbuildpilot.environment.PathManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetaDefenderScannerTest {

    @TempDir
    Path tempDir;

    private HttpServer server;
    private int port;
    private String mockVerdict;
    private int mockProgress;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        server.createContext("/v4/file", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "{\"data_id\":\"mock-id\"}";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } else if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"progress_percentage\":" + mockProgress + 
                                  ",\"scan_results\":{\"scan_all_result_a\":\"" + mockVerdict + "\"}}";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
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
    }

    @Test
    void testMissingApiKeyThrows() {
        // Since we don't set JBUILDPILOT_METADEFENDER_APIKEY and no config exists
        ApiKeyProvider provider = new ApiKeyProvider(new PathManager(tempDir));
        MetaDefenderScanner scanner = new MetaDefenderScanner(provider, "http://127.0.0.1:" + port);
        
        assertThrows(IllegalStateException.class, provider::getApiKey);
    }

    @Test
    void testCleanVerdict() throws Exception {
        Path dummy = tempDir.resolve("dummy.jar");
        Files.writeString(dummy, "mock bytes");

        // Mock config.properties to bypass env var
        Path config = tempDir.resolve("config.properties");
        Files.writeString(config, "metadefender.apikey=mock-key-123\n");

        ApiKeyProvider provider = new ApiKeyProvider(new PathManager(tempDir));
        MetaDefenderScanner scanner = new MetaDefenderScanner(provider, "http://127.0.0.1:" + port);

        // Setup mock response
        mockProgress = 100;
        mockVerdict = "Clean";

        assertEquals(ScanVerdict.CLEAN, scanner.scanFile(dummy));
    }

    @Test
    void testMaliciousVerdict() throws Exception {
        Path dummy = tempDir.resolve("dummy.jar");
        Files.writeString(dummy, "mock bytes");
        Path config = tempDir.resolve("config.properties");
        Files.writeString(config, "metadefender.apikey=mock-key-123\n");

        ApiKeyProvider provider = new ApiKeyProvider(new PathManager(tempDir));
        MetaDefenderScanner scanner = new MetaDefenderScanner(provider, "http://127.0.0.1:" + port);

        mockProgress = 100;
        mockVerdict = "Infected";

        assertEquals(ScanVerdict.MALICIOUS, scanner.scanFile(dummy));
    }

    @Test
    void testSuspiciousVerdict() throws Exception {
        Path dummy = tempDir.resolve("dummy.jar");
        Files.writeString(dummy, "mock bytes");
        Path config = tempDir.resolve("config.properties");
        Files.writeString(config, "metadefender.apikey=mock-key-123\n");

        ApiKeyProvider provider = new ApiKeyProvider(new PathManager(tempDir));
        MetaDefenderScanner scanner = new MetaDefenderScanner(provider, "http://127.0.0.1:" + port);

        mockProgress = 100;
        mockVerdict = "Suspicious";

        assertEquals(ScanVerdict.SUSPICIOUS, scanner.scanFile(dummy));
    }
}
