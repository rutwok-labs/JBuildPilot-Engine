package io.github.rutwoklabs.jbuildpilot.cli.command;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionRequest;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.cli.TerminalPermissionHandler;
import io.github.rutwoklabs.jbuildpilot.downloader.*;
import io.github.rutwoklabs.jbuildpilot.environment.*;
import io.github.rutwoklabs.jbuildpilot.security.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ToolCommand {
    private final String action;
    private final String tool;
    private final String hash;

    public ToolCommand(String action, String tool, String hash) {
        this.action = action;
        this.tool = tool;
        this.hash = hash;
    }

    public void execute() {
        if ("install".equals(action)) {
            System.out.println("Installing tool: " + tool);
            TerminalPermissionHandler handler = new TerminalPermissionHandler();
            PermissionDecision decision = handler.request(
                new PermissionRequest(
                    PermissionType.DOWNLOAD_TOOL, 
                    "Install " + tool, 
                    "User requested explicit tool install.", 
                    tool, 
                    null, 
                    0, 
                    null, 
                    false, 
                    java.util.Collections.emptyMap()
                )
            );
            
            if (decision == PermissionDecision.APPROVE) {
                try {
                    PathManager pathManager = new PathManager(Paths.get(System.getProperty("user.home"), ".jbuildpilot"));
                    VirtualSpace virtualSpace = new VirtualSpace(pathManager);
                    virtualSpace.initialize();

                    DownloadCache cache = new DownloadCache(virtualSpace);
                    ChecksumVerifier verifier = new ChecksumVerifier();
                    
                    ApiKeyProvider apiKeyProvider = new ApiKeyProvider(pathManager);
                    MalwareScanner scanner = new MetaDefenderScanner(apiKeyProvider);
                    
                    Downloader downloader = new Downloader(cache, verifier, scanner, SecurityPolicy.strict());
                    ArtifactRepository repo = new TrustedOfficialRepository();

                    Artifact artifact = new Artifact(tool, hash);
                    DownloadResult dRes = downloader.download(new DownloadRequest(artifact, repo));
                    
                    if (!dRes.isSuccessful()) {
                        System.err.println("Installation failed during download/scan: " + dRes.getErrorMessage());
                        return;
                    }

                    System.out.println("Extracting and installing tool...");
                    ToolInstaller installer = new ToolInstaller(pathManager);
                    Path installDir = installer.installTool(tool, dRes.getCachedPath());

                    System.out.println("Tool installed successfully in " + installDir);
                } catch (Exception e) {
                    System.err.println("Installation failed: " + e.getMessage());
                }
            } else {
                System.out.println("Installation aborted.");
            }
        } else {
            System.err.println("Unknown tool action: " + action);
        }
    }
}
