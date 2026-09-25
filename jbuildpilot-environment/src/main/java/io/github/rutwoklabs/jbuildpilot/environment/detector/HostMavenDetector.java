package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.Version;
import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class HostMavenDetector implements MavenDetector {
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public ToolInfo detect(VersionConstraint constraint) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"mvn", "-v"});
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                line = reader.readLine();
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return ToolInfo.missing("maven");
            }

            if (line != null && line.contains("Apache Maven")) {
                // E.g., "Apache Maven 3.9.6 (..." -> "3.9.6"
                String[] parts = line.split(" ");
                String version = parts.length > 2 ? parts[2] : "unknown";
                if (constraint != null && !"unknown".equals(version) && !constraint.isSatisfiedBy(new Version(version))) {
                    return ToolInfo.incompatible("maven", version, null);
                }
                return ToolInfo.available("maven", version, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Ignored, fallback to missing
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return ToolInfo.missing("maven");
    }
}
