package io.github.rutwoklabs.jbuildpilot.github;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lightweight GitHub API client using native Java 11 HttpClient.
 */
public class GitHubClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String token;
    private final HttpClient httpClient;

    public GitHubClient(String token) {
        this.token = token;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public String getIssue(String repository, String issueId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repository + "/issues/" + issueId))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch issue. Status: " + response.statusCode());
        }
        return response.body();
    }
    
    public void postComment(String repository, String issueId, String body) throws IOException, InterruptedException {
        String jsonPayload = "{\"body\":\"" + body.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repository + "/issues/" + issueId + "/comments"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException("Failed to post comment. Status: " + response.statusCode() + " " + response.body());
        }
    }
}
