package io.github.rutwoklabs.jbuildpilot.downloader;

import io.github.rutwoklabs.jbuildpilot.security.MalwareScanner;
import io.github.rutwoklabs.jbuildpilot.security.ScanVerdict;
import io.github.rutwoklabs.jbuildpilot.security.SecurityPolicy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Downloader {
    private final DownloadCache cache;
    private final ChecksumVerifier checksumVerifier;
    private final HttpClient httpClient;
    private final MalwareScanner malwareScanner;
    private final SecurityPolicy securityPolicy;
    private static final int MAX_RETRIES = 3;

    public Downloader(DownloadCache cache, ChecksumVerifier checksumVerifier, MalwareScanner malwareScanner, SecurityPolicy securityPolicy) {
        this.cache = cache;
        this.checksumVerifier = checksumVerifier;
        this.malwareScanner = malwareScanner;
        this.securityPolicy = securityPolicy;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // Task 4: Prevent transparent redirects to HTTP
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
        // Task 3: Warn if non-strict
        if (securityPolicy.isSkipScanOnFailure() || securityPolicy.isForceSuspicious()) {
            System.err.println("WARNING: running with relaxed security policy (skip-scan-on-failure=" 
                + securityPolicy.isSkipScanOnFailure() + ", force-suspicious=" 
                + securityPolicy.isForceSuspicious() + ")");
        }
    }

    private void validateScheme(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase("https") && !uri.getHost().equals("localhost") && !uri.getHost().equals("127.0.0.1")) {
            throw new SecurityException("Insecure HTTP protocol is strictly forbidden. Must use HTTPS: " + uri);
        }
    }

    public DownloadResult download(DownloadRequest request) {
        Artifact artifact = request.getArtifact();

        // 1. Check Cache
        if (cache.isCached(artifact)) {
            Path cached = cache.getCachePath(artifact);
            if (checksumVerifier.verify(cached, artifact.getExpectedChecksum())) {
                return DownloadResult.success(cached);
            }
            try {
                Files.deleteIfExists(cached);
            } catch (IOException e) {
                return DownloadResult.failure("Corrupted cache entry could not be cleared: " + e.getMessage());
            }
        }

        URI originalUri = request.getRepository().resolveUri(artifact);
        if (originalUri == null) {
            return DownloadResult.failure("Repository could not resolve URI for artifact.");
        }

        // 2. Download to Temp File with Retries and manual redirects
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jbuildpilot-dl-", ".tmp");
            boolean success = false;
            String lastError = "";

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    URI currentUri = originalUri;
                    int redirects = 0;
                    HttpResponse<Path> response = null;

                    while (redirects < 5) {
                        validateScheme(currentUri); // Task 4: Re-validate scheme on every hop
                        
                        HttpRequest httpRequest = HttpRequest.newBuilder(currentUri)
                                .timeout(Duration.ofMinutes(5))
                                .GET()
                                .build();
                        response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofFile(tempFile));

                        int status = response.statusCode();
                        if (status >= 300 && status <= 399) {
                            String location = response.headers().firstValue("Location").orElse(null);
                            if (location == null) {
                                throw new IOException("Redirect requested but no Location header found.");
                            }
                            currentUri = currentUri.resolve(location);
                            redirects++;
                        } else {
                            break;
                        }
                    }

                    if (response != null && response.statusCode() == 200) {
                        success = true;
                        break;
                    } else {
                        lastError = "HTTP " + (response != null ? response.statusCode() : "unknown");
                    }
                } catch (Exception e) {
                    lastError = e.getMessage();
                }

                if (!success && attempt < MAX_RETRIES) {
                    Thread.sleep(500 * attempt);
                }
            }

            if (!success) {
                return DownloadResult.failure("Download failed after " + MAX_RETRIES + " attempts. Last error: " + lastError);
            }

            // 3. Verify Checksum of downloaded temp file
            if (!checksumVerifier.verify(tempFile, artifact.getExpectedChecksum())) {
                return DownloadResult.failure("Checksum validation failed for downloaded artifact.");
            }

            // 4. Scan for Malware
            ScanVerdict verdict = malwareScanner.scanFile(tempFile);
            switch (verdict) {
                case CLEAN:
                    break;
                case MALICIOUS:
                    throw new SecurityException("Artifact " + artifact.getId() + " flagged as MALICIOUS.");
                case SUSPICIOUS:
                    if (!securityPolicy.isForceSuspicious()) {
                        throw new SecurityException("Artifact " + artifact.getId() + " flagged as SUSPICIOUS. Use force policy to allow.");
                    }
                    System.err.println("WARNING: Artifact " + artifact.getId() + " flagged as SUSPICIOUS. Force policy enabled, proceeding.");
                    break;
                case SCAN_FAILED:
                case UNKNOWN:
                    if (!securityPolicy.isSkipScanOnFailure()) {
                        throw new SecurityException("Malware scan failed or returned UNKNOWN for " + artifact.getId() + ". Blocking by default.");
                    }
                    System.err.println("WARNING: Malware scan failed for " + artifact.getId() + ". Skip-scan policy enabled, proceeding.");
                    break;
            }

            // 5. Atomic Move to Cache
            cache.storeAtomically(tempFile, artifact);
            return DownloadResult.success(cache.getCachePath(artifact));

        } catch (Exception e) {
            return DownloadResult.failure("Critical download exception: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }
}
