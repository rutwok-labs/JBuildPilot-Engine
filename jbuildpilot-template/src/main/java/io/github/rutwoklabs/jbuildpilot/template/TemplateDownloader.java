package io.github.rutwoklabs.jbuildpilot.template;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public class TemplateDownloader {
    private final Path cacheDir;

    public TemplateDownloader(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public Path downloadToCache(TemplateRepository repository, String templateId) throws TemplateException {
        // Fetch from repository
        Path sourceTemplateDir = repository.fetchTemplate(templateId);

        // Define cache target
        Path targetDir = cacheDir.resolve(templateId);
        
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // Simple recursive copy (assuming Local repository for now)
            // In the future this would handle HTTP downloads and extract ZIP safely.
            copyFolder(sourceTemplateDir, targetDir);
            
            return targetDir;
        } catch (IOException e) {
            throw new TemplateException("Failed to cache template: " + templateId, e);
        }
    }

    private void copyFolder(Path source, Path target) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
            for (Path sourcePath : (Iterable<Path>) paths::iterator) {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) Files.createDirectory(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
