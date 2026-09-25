package io.github.rutwoklabs.jbuildpilot.analyzer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects the build system without executing any project code.
 */
public final class BuildSystemDetector {

    public BuildSystemType detect(Path projectDir) {
        if (Files.exists(projectDir.resolve("pom.xml"))) {
            return BuildSystemType.MAVEN;
        }

        if (Files.exists(projectDir.resolve("build.gradle")) ||
            Files.exists(projectDir.resolve("build.gradle.kts")) ||
            Files.exists(projectDir.resolve("settings.gradle")) ||
            Files.exists(projectDir.resolve("settings.gradle.kts"))) {
            return BuildSystemType.GRADLE;
        }

        return BuildSystemType.UNKNOWN;
    }
}
