package io.github.rutwoklabs.jbuildpilot.builder;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildEngine extends AbstractBuildEngine {

    public MavenBuildEngine(PermissionManager permissionManager, ProcessRunner processRunner) {
        super(permissionManager, processRunner);
    }

    @Override
    protected List<String> buildCommand(BuildRequest request) {
        List<String> command = new ArrayList<>();
        
        String executable;
        if (request.isUseWrapper()) {
            executable = isWindows() ? "mvnw.cmd" : "./mvnw";
            command.add(request.getProjectDirectory().resolve(executable).toAbsolutePath().toString());
        } else {
            executable = isWindows() ? "mvn.cmd" : "mvn";
            Path toolHome = request.getToolHome();
            if (toolHome != null) {
                command.add(toolHome.resolve("bin").resolve(executable).toAbsolutePath().toString());
            } else {
                command.add(executable); // Fallback to system path
            }
        }

        // Add user requested arguments safely (handled by ProcessBuilder escaping natively)
        command.addAll(request.getArguments());

        return command;
    }
}
