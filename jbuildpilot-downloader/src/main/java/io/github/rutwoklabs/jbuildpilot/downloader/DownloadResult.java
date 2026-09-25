package io.github.rutwoklabs.jbuildpilot.downloader;

import java.nio.file.Path;

public class DownloadResult {
    private final boolean successful;
    private final Path cachedPath;
    private final String errorMessage;

    public DownloadResult(boolean successful, Path cachedPath, String errorMessage) {
        this.successful = successful;
        this.cachedPath = cachedPath;
        this.errorMessage = errorMessage;
    }

    public static DownloadResult success(Path path) {
        return new DownloadResult(true, path, null);
    }

    public static DownloadResult failure(String message) {
        return new DownloadResult(false, null, message);
    }

    public boolean isSuccessful() { return successful; }
    public Path getCachedPath() { return cachedPath; }
    public String getErrorMessage() { return errorMessage; }
}
