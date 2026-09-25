package io.github.rutwoklabs.jbuildpilot.template;

import io.github.rutwoklabs.jbuildpilot.template.model.TemplateRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTemplateRepositoryTest {

    @Test
    void testFetchRegistry(@TempDir Path tempDir) throws Exception {
        String json = "{\"schemaVersion\": 1, \"repositoryVersion\": \"1.0\", \"templates\": [{\"id\":\"test-id\"}]}";
        Files.writeString(tempDir.resolve("registry.json"), json);

        LocalTemplateRepository repo = new LocalTemplateRepository(tempDir);
        TemplateRegistry registry = repo.fetchRegistry();

        assertEquals(1, registry.getSchemaVersion());
        assertEquals("1.0", registry.getRepositoryVersion());
        assertEquals(1, registry.getTemplates().size());
        assertEquals("test-id", registry.getTemplates().get(0).getId());
    }

    @Test
    void testFetchTemplate(@TempDir Path tempDir) throws Exception {
        String json = "{\"templates\": [{\"id\":\"t1\", \"path\":\"templates/t1\"}]}";
        Files.writeString(tempDir.resolve("registry.json"), json);
        Path t1Dir = tempDir.resolve("templates/t1");
        Files.createDirectories(t1Dir);

        LocalTemplateRepository repo = new LocalTemplateRepository(tempDir);
        Path fetched = repo.fetchTemplate("t1");

        assertEquals(t1Dir, fetched);
    }
}
