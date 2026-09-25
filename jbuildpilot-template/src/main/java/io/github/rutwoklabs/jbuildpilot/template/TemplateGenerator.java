package io.github.rutwoklabs.jbuildpilot.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateGenerator {
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_]+)\\}\\}");

    public void generate(Path sourceTemplate, Path targetProjectDir, Map<String, String> originalVariables) throws TemplateException {
        try {
            Map<String, String> variables = new java.util.HashMap<>(originalVariables);
            if (!Files.exists(targetProjectDir)) {
                Files.createDirectories(targetProjectDir);
            }
            
            // Auto-compute PACKAGE_PATH if PACKAGE_NAME is provided
            if (variables.containsKey("PACKAGE_NAME") && !variables.containsKey("PACKAGE_PATH")) {
                variables.put("PACKAGE_PATH", variables.get("PACKAGE_NAME").replace('.', '/'));
            }

            Files.walkFileTree(sourceTemplate, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = sourceTemplate.relativize(dir);
                    if (relative.toString().isEmpty()) return FileVisitResult.CONTINUE;

                    String replacedRelative = substituteString(relative.toString(), variables);
                    Path targetDir = targetProjectDir.resolve(replacedRelative).normalize();
                    
                    // Path traversal protection
                    if (!targetDir.startsWith(targetProjectDir.normalize())) {
                        throw new SecurityException("Path traversal detected in template directory resolution.");
                    }
                    
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Do not copy template.json or registry.json into the target project
                    if (file.getFileName().toString().equals("template.json")) {
                        return FileVisitResult.CONTINUE;
                    }

                    Path relative = sourceTemplate.relativize(file);
                    String replacedRelative = substituteString(relative.toString(), variables);
                    Path targetFile = targetProjectDir.resolve(replacedRelative).normalize();
                    
                    // Path traversal protection
                    if (!targetFile.startsWith(targetProjectDir.normalize())) {
                        throw new SecurityException("Path traversal detected in template file resolution.");
                    }

                    if (isTextFile(file)) {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        String replacedContent = substituteString(content, variables);
                        Files.writeString(targetFile, replacedContent, StandardCharsets.UTF_8);
                    } else {
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new TemplateException("Failed to generate project from template: " + e.getMessage(), e);
        }
    }

    private String substituteString(String input, Map<String, String> variables) {
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, matcher.group(0));
            // Security: sanitize path traversal attempts in variables
            replacement = replacement.replace("..", "").replace("\\", "/");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".json") || 
               name.endsWith(".md") || name.endsWith(".gradle") || name.endsWith(".kts") ||
               name.endsWith(".properties") || name.endsWith(".jbe");
    }
}
