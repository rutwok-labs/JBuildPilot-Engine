package io.github.rutwoklabs.jbuildpilot.template.model;

import java.util.List;
import java.util.Map;

public class TemplateMetadata {
    private String id;
    private String name;
    private String version;
    private String description;
    private String type;
    private Map<String, String> requirements;
    private List<TemplateVariable> variables;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getRequirements() { return requirements; }
    public void setRequirements(Map<String, String> requirements) { this.requirements = requirements; }
    public List<TemplateVariable> getVariables() { return variables; }
    public void setVariables(List<TemplateVariable> variables) { this.variables = variables; }
}
