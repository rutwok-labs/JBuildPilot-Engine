package io.github.rutwoklabs.jbuildpilot.core.permission;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class PermissionRequest {
    private final PermissionType type;
    private final String description;
    private final String reason;
    private final String toolName;
    private final String toolVersion;
    private final long downloadSize;
    private final Path targetPath;
    private final boolean requiresAdministrator;
    private final Map<String, String> metadata;

    public PermissionRequest(PermissionType type, String description) {
        this(type, description, null, null, null, 0, null, false, Collections.emptyMap());
    }

    public PermissionRequest(PermissionType type, String description, String reason, String toolName, String toolVersion, long downloadSize, Path targetPath, boolean requiresAdministrator, Map<String, String> metadata) {
        this.type = type;
        this.description = description;
        this.reason = reason;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.downloadSize = downloadSize;
        this.targetPath = targetPath;
        this.requiresAdministrator = requiresAdministrator;
        this.metadata = metadata != null ? metadata : Collections.emptyMap();
    }

    public PermissionType type() { return type; }
    public String description() { return description; }
    public String reason() { return reason; }
    public String toolName() { return toolName; }
    public String toolVersion() { return toolVersion; }
    public long downloadSize() { return downloadSize; }
    public Path targetPath() { return targetPath; }
    public boolean requiresAdministrator() { return requiresAdministrator; }
    public Map<String, String> metadata() { return metadata; }
}
