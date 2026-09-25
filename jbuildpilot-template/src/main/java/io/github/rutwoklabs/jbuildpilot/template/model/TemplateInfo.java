package io.github.rutwoklabs.jbuildpilot.template.model;

import java.util.List;
import java.util.Map;

public class TemplateInfo {
    private String id;
    private String name;
    private String version;
    private String description;
    private String path;
    private String category;
    private List<String> tags;
    private Map<String, String> java;
    private String buildSystem;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Map<String, String> getJava() { return java; }
    public void setJava(Map<String, String> java) { this.java = java; }
    public String getBuildSystem() { return buildSystem; }
    public void setBuildSystem(String buildSystem) { this.buildSystem = buildSystem; }
}
