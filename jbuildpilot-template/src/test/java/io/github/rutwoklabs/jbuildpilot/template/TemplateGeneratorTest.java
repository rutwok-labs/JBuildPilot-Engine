package io.github.rutwoklabs.jbuildpilot.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateGeneratorTest {

    @Test
    void testVariableSubstitutionAndGeneration(@TempDir Path source, @TempDir Path target) throws Exception {
        // Create template
        Path pkgDir = source.resolve("src/main/java/{{PACKAGE_PATH}}");
        Files.createDirectories(pkgDir);
        Files.writeString(pkgDir.resolve("{{MAIN_CLASS}}.java"), "package {{PACKAGE_NAME}}; class {{MAIN_CLASS}} {}");

        TemplateGenerator generator = new TemplateGenerator();
        generator.generate(source, target, Map.of(
            "PACKAGE_NAME", "com.example.test",
            "MAIN_CLASS", "App"
        ));

        Path targetFile = target.resolve("src/main/java/com/example/test/App.java");
        assertTrue(Files.exists(targetFile));
        String content = Files.readString(targetFile);
        assertEquals("package com.example.test; class App {}", content);
    }

    @Test
    void testPathTraversalPrevention(@TempDir Path source, @TempDir Path target) throws Exception {
        Path maliciousDir = source.resolve("normal");
        Files.createDirectories(maliciousDir);
        Files.writeString(maliciousDir.resolve("file.txt"), "hello");

        TemplateGenerator generator = new TemplateGenerator();
        
        // Try to substitute a variable that contains path traversal
        assertDoesNotThrow(() -> {
            generator.generate(source, target, Map.of("VAR", "../../hacked"));
        });
        
        // Ensure nothing escaped target directory
        assertFalse(Files.exists(target.getParent().resolve("hacked")));
    }
}
