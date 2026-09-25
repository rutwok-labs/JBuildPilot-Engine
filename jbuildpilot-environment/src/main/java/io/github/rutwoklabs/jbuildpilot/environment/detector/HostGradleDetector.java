package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.Version;
import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class HostGradleDetector implements GradleDetector {
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public ToolInfo detect(VersionConstraint constraint) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"gradle", "-v"});
            String version = "unknown";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Gradle ")) {
                        version = line.substring(7).trim();
                        break;
                    }
                }
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return ToolInfo.missing("gradle");
            }

            if (!"unknown".equals(version)) {
                if (constraint != null && !constraint.isSatisfiedBy(new Version(version))) {
                    return ToolInfo.incompatible("gradle", version, null);
                }
                return ToolInfo.available("gradle", version, null);
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
        return ToolInfo.missing("gradle");
    }
}
