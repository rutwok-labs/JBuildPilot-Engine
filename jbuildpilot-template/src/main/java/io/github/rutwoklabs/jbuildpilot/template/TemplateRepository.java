package io.github.rutwoklabs.jbuildpilot.template;

import io.github.rutwoklabs.jbuildpilot.template.model.TemplateRegistry;
import java.nio.file.Path;

public interface TemplateRepository {
    TemplateRegistry fetchRegistry() throws TemplateException;
    Path fetchTemplate(String templateId) throws TemplateException;
}
