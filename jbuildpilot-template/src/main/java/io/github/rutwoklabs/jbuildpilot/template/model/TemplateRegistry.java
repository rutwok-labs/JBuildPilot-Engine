package io.github.rutwoklabs.jbuildpilot.template.model;

import java.util.List;

public class TemplateRegistry {
    private int schemaVersion;
    private String repositoryVersion;
    private List<TemplateInfo> templates;

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getRepositoryVersion() { return repositoryVersion; }
    public void setRepositoryVersion(String repositoryVersion) { this.repositoryVersion = repositoryVersion; }
    public List<TemplateInfo> getTemplates() { return templates; }
    public void setTemplates(List<TemplateInfo> templates) { this.templates = templates; }
}
