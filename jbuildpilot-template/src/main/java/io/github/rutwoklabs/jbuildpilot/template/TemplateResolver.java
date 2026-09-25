package io.github.rutwoklabs.jbuildpilot.template;

import io.github.rutwoklabs.jbuildpilot.template.model.TemplateInfo;
import io.github.rutwoklabs.jbuildpilot.template.model.TemplateRegistry;

import java.util.List;
import java.util.stream.Collectors;

public class TemplateResolver {
    private final TemplateRegistry registry;

    public TemplateResolver(TemplateRegistry registry) {
        this.registry = registry;
    }

    public List<TemplateInfo> search(String query) {
        if (query == null || query.isBlank()) {
            return registry.getTemplates();
        }
        
        String lowerQuery = query.toLowerCase();
        return registry.getTemplates().stream()
            .filter(t -> matches(t, lowerQuery))
            .collect(Collectors.toList());
    }
    
    private boolean matches(TemplateInfo t, String query) {
        if (t.getId() != null && t.getId().toLowerCase().contains(query)) return true;
        if (t.getName() != null && t.getName().toLowerCase().contains(query)) return true;
        if (t.getCategory() != null && t.getCategory().toLowerCase().contains(query)) return true;
        if (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(query))) return true;
        return false;
    }
}
