package io.github.rutwoklabs.jbuildpilot.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rutwoklabs.jbuildpilot.template.model.TemplateRegistry;
import io.github.rutwoklabs.jbuildpilot.template.model.TemplateInfo;

import java.nio.file.Files;
import java.nio.file.Path;

public class LocalTemplateRepository implements TemplateRepository {
    private final Path repositoryRoot;
    private final ObjectMapper mapper;

    public LocalTemplateRepository(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
        this.mapper = new ObjectMapper();
    }

    @Override
    public TemplateRegistry fetchRegistry() throws TemplateException {
        Path registryPath = repositoryRoot.resolve("registry.json");
        if (!Files.exists(registryPath)) {
            throw new TemplateException("Registry not found at: " + registryPath);
        }
        try {
            return mapper.readValue(registryPath.toFile(), TemplateRegistry.class);
        } catch (Exception e) {
            throw new TemplateException("Failed to parse registry: " + e.getMessage(), e);
        }
    }

    @Override
    public Path fetchTemplate(String templateId) throws TemplateException {
        TemplateRegistry registry = fetchRegistry();
        TemplateInfo info = registry.getTemplates().stream()
                .filter(t -> t.getId().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new TemplateException("Template not found: " + templateId));

        Path templatePath = repositoryRoot.resolve(info.getPath());
        if (!Files.exists(templatePath) || !Files.isDirectory(templatePath)) {
            throw new TemplateException("Template path does not exist: " + templatePath);
        }
        return templatePath;
    }
}
