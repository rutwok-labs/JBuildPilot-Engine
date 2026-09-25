package io.github.rutwoklabs.jbuildpilot.analyzer;

import io.github.rutwoklabs.jbuildpilot.core.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

public final class GradleAnalyzer implements JavaDetector {

    private static final Pattern SOURCE_COMPAT_PATTERN = Pattern.compile("sourceCompatibility\\s*=?\\s*['\"]?([a-zA-Z0-9._]+)['\"]?");
    private static final Pattern TOOLCHAIN_PATTERN = Pattern.compile("languageVersion\\s*=?\\s*JavaLanguageVersion\\.of\\(\\s*['\"]?([0-9]+)['\"]?\\s*\\)");
    
    // Very naive regexes for phase 2. Real parser needed for complex files.
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("(implementation|api|compileOnly|runtimeOnly|testImplementation)\\s+['\"]([^:]+):([^:]+):?([^'\"]*)['\"]");
    private static final Pattern PLUGIN_PATTERN = Pattern.compile("id\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?(?:\\s*version\\s*['\"]([^'\"]+)['\"])?");

    public ProjectModel analyze(Path projectDir) {
        ProjectMetadata metadata = extractMetadata(projectDir);
        List<Requirement> requirements = new ArrayList<>();
        // 1. Record Build System
        requirements.add(new GradleRequirement(VersionConstraint.any(), RequirementStatus.UNKNOWN));

        detectJavaVersion(projectDir).ifPresent(requirements::add);

        // Parse build files for dependencies and plugins
        String[] filesToCheck = {"build.gradle", "build.gradle.kts"};
        for (String fileName : filesToCheck) {
            Path file = projectDir.resolve(fileName);
            if (Files.exists(file)) {
                try {
                    String content = Files.readString(file);
                    requirements.addAll(extractDependencies(content));
                    requirements.addAll(extractPlugins(content));
                } catch (IOException ignored) {}
            }
        }

        return new ProjectModel(metadata, requirements);
    }

    private ProjectMetadata extractMetadata(Path projectDir) {
        String name = projectDir.getFileName().toString();
        
        // Multi-module detection (settings.gradle include)
        int moduleCount = countModules(projectDir);

        // Source file count
        long sourceFiles = countSourceFiles(projectDir);

        String descriptionText = buildDescription(moduleCount, sourceFiles);
        return new ProjectMetadata(name, null, null, descriptionText);
    }

    private int countModules(Path projectDir) {
        int count = 0;
        String[] settingsFiles = {"settings.gradle", "settings.gradle.kts"};
        for (String sf : settingsFiles) {
            Path file = projectDir.resolve(sf);
            if (Files.exists(file)) {
                try {
                    String content = Files.readString(file);
                    Matcher m = Pattern.compile("include\\s*\\(?\\s*['\"]([^'\"]+)['\"]").matcher(content);
                    while (m.find()) count++;
                } catch (IOException ignored) {}
            }
        }
        return count;
    }

    private String buildDescription(int modules, long sourceFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("build=gradle");
        if (modules > 0) sb.append(" | subprojects=").append(modules);
        sb.append(" | sources=").append(sourceFiles);
        return sb.toString();
    }

    private long countSourceFiles(Path projectDir) {
        Path srcMain = projectDir.resolve("src/main/java");
        if (!Files.exists(srcMain)) return 0;
        try (var stream = Files.walk(srcMain)) {
            return stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<DependencyRequirement> extractDependencies(String content) {
        List<DependencyRequirement> result = new ArrayList<>();
        Matcher m = DEPENDENCY_PATTERN.matcher(content);
        while (m.find()) {
            String scope = m.group(1);
            String gId = m.group(2);
            String aId = m.group(3);
            String ver = m.group(4);
            
            String identity = scope.startsWith("test") ? gId + ":" + aId + " [" + scope + "]" : gId + ":" + aId;
            VersionConstraint vc = (ver == null || ver.isEmpty()) ? VersionConstraint.any() : VersionConstraint.exact(ver);
            
            // Note: The identity logic should strictly be gId:aId, but we are annotating scope for CLI.
            // For a robust implementation, Requirement should have a getScope() or getTags() method.
            result.add(new DependencyRequirement(gId, aId, vc, RequirementStatus.UNKNOWN));
        }
        return result;
    }

    private List<PluginRequirement> extractPlugins(String content) {
        List<PluginRequirement> result = new ArrayList<>();
        // Extract content inside plugins { ... }
        Matcher blockMatcher = Pattern.compile("plugins\\s*\\{([^{}]+)\\}").matcher(content);
        while (blockMatcher.find()) {
            String block = blockMatcher.group(1);
            Matcher pMatcher = PLUGIN_PATTERN.matcher(block);
            while (pMatcher.find()) {
                String id = pMatcher.group(1);
                String ver = pMatcher.group(2);
                VersionConstraint vc = (ver == null) ? VersionConstraint.any() : VersionConstraint.exact(ver);
                result.add(new PluginRequirement(id, vc, RequirementStatus.UNKNOWN));
            }
        }
        return result;
    }

    @Override
    public Optional<JavaRequirement> detectJavaVersion(Path projectDir) {
        String[] filesToCheck = {"build.gradle", "build.gradle.kts", "gradle.properties"};
        for (String fileName : filesToCheck) {
            Path file = projectDir.resolve(fileName);
            if (Files.exists(file)) {
                try {
                    String content = Files.readString(file);
                    Matcher toolchainMatcher = TOOLCHAIN_PATTERN.matcher(content);
                    if (toolchainMatcher.find()) return Optional.of(new JavaRequirement(toolchainMatcher.group(1)));

                    Matcher sourceCompatMatcher = SOURCE_COMPAT_PATTERN.matcher(content);
                    if (sourceCompatMatcher.find()) {
                        String version = sourceCompatMatcher.group(1);
                        if (version.startsWith("JavaVersion.VERSION_")) {
                            version = version.replace("JavaVersion.VERSION_", "").replace("_", ".");
                        }
                        return Optional.of(new JavaRequirement(version));
                    }
                } catch (IOException ignored) {}
            }
        }
        return Optional.empty();
    }
}
