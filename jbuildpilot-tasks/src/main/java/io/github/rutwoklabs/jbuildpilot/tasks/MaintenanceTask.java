package io.github.rutwoklabs.jbuildpilot.tasks;

import java.util.List;

public class MaintenanceTask {
    private final String id;
    private final String type;
    private final String targetPackage;
    private final String currentVersion;
    private final String targetVersion;
    private final RiskLevel risk;
    private final String reason;
    private final List<String> files;
    private final boolean requiresApproval;
    
    private boolean approved = false;

    public MaintenanceTask(String id, String type, String targetPackage, String currentVersion, String targetVersion, RiskLevel risk, String reason, List<String> files, boolean requiresApproval) {
        this.id = id;
        this.type = type;
        this.targetPackage = targetPackage;
        this.currentVersion = currentVersion;
        this.targetVersion = targetVersion;
        this.risk = risk;
        this.reason = reason;
        this.files = files;
        this.requiresApproval = requiresApproval;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTargetPackage() { return targetPackage; }
    public String getCurrentVersion() { return currentVersion; }
    public String getTargetVersion() { return targetVersion; }
    public RiskLevel getRisk() { return risk; }
    public String getReason() { return reason; }
    public List<String> getFiles() { return files; }
    public boolean isRequiresApproval() { return requiresApproval; }
    
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s %s %s -> %s (Risk: %s)", id, type, targetPackage, currentVersion, targetVersion, risk);
    }
}
