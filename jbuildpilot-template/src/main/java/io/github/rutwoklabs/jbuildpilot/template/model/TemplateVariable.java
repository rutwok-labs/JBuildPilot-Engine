package io.github.rutwoklabs.jbuildpilot.template.model;

public class TemplateVariable {
    private String name;
    private boolean required;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
