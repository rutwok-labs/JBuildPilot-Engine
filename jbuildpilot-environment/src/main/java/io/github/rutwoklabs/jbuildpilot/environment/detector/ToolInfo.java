package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.RequirementStatus;

import java.nio.file.Path;

public class ToolInfo {
    private final String toolName;
    private final String version;
    private final Path executablePath;
    private final RequirementStatus status;

    public ToolInfo(String toolName, String version, Path executablePath, RequirementStatus status) {
        this.toolName = toolName;
        this.version = version;
        this.executablePath = executablePath;
        this.status = status;
    }

    public static ToolInfo missing(String toolName) {
        return new ToolInfo(toolName, null, null, RequirementStatus.MISSING);
    }

    public static ToolInfo incompatible(String toolName, String version, Path executablePath) {
        return new ToolInfo(toolName, version, executablePath, RequirementStatus.INCOMPATIBLE);
    }

    public static ToolInfo available(String toolName, String version, Path executablePath) {
        return new ToolInfo(toolName, version, executablePath, RequirementStatus.AVAILABLE);
    }

    public String getToolName() { return toolName; }
    public String getVersion() { return version; }
    public Path getExecutablePath() { return executablePath; }
    public RequirementStatus getStatus() { return status; }
}
