package io.github.rutwoklabs.jbuildpilot.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaDefenderScanner implements MalwareScanner {

    private final ApiKeyProvider apiKeyProvider;
    private final String baseUrl;
    private final HttpClient httpClient;

    // Default polling config
    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_DELAY_MS = 2000;

    public MetaDefenderScanner(ApiKeyProvider apiKeyProvider) {
        this(apiKeyProvider, "https://api.metadefender.com");
    }

    public MetaDefenderScanner(ApiKeyProvider apiKeyProvider, String baseUrl) {
        this.apiKeyProvider = apiKeyProvider;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public ScanVerdict scanFile(Path file) {
        try {
            String apiKey = apiKeyProvider.getApiKey();

            // Step 1: POST file bytes to get data_id
            HttpRequest uploadReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v4/file"))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/octet-stream")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .build();

            HttpResponse<String> uploadRes = httpClient.send(uploadReq, HttpResponse.BodyHandlers.ofString());
            if (uploadRes.statusCode() != 200) {
                return ScanVerdict.SCAN_FAILED;
            }

            String dataId = extractJsonValue(uploadRes.body(), "data_id");
            if (dataId == null) {
                return ScanVerdict.SCAN_FAILED;
            }

            // Step 2: Poll GET /v4/file/{data_id} until progress == 100
            for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
                Thread.sleep(POLL_DELAY_MS);

                HttpRequest pollReq = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v4/file/" + dataId))
                        .header("apikey", apiKey)
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();

                HttpResponse<String> pollRes = httpClient.send(pollReq, HttpResponse.BodyHandlers.ofString());
                if (pollRes.statusCode() != 200) {
                    return ScanVerdict.SCAN_FAILED; // Explicitly fail, never silent default
                }

                String progressStr = extractJsonValue(pollRes.body(), "progress_percentage");
                if ("100".equals(progressStr)) {
                    String result = extractJsonValue(pollRes.body(), "scan_all_result_a");
                    return mapVerdict(result);
                }
            }

            // Timeout reached
            return ScanVerdict.SCAN_FAILED;

        } catch (Exception e) {
            return ScanVerdict.SCAN_FAILED;
        }
    }

    private String extractJsonValue(String json, String key) {
        // A lightweight JSON string extractor to avoid Jackson dependency.
        // Matches "key":"value" or "key":value.
        Pattern strPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher strMatcher = strPattern.matcher(json);
        if (strMatcher.find()) {
            return strMatcher.group(1);
        }

        Pattern numPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9]+)");
        Matcher numMatcher = numPattern.matcher(json);
        if (numMatcher.find()) {
            return numMatcher.group(1);
        }

        return null;
    }

    private ScanVerdict mapVerdict(String scanAllResultA) {
        if (scanAllResultA == null) return ScanVerdict.UNKNOWN;

        String val = scanAllResultA.toLowerCase();
        switch (val) {
            case "clean":
            case "no threat detected":
                return ScanVerdict.CLEAN;
            case "infected":
            case "malicious":
                return ScanVerdict.MALICIOUS;
            case "suspicious":
                return ScanVerdict.SUSPICIOUS;
            case "failed":
            case "aborted":
                return ScanVerdict.SCAN_FAILED;
            default:
                return ScanVerdict.UNKNOWN;
        }
    }
}
