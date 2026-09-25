package io.github.rutwoklabs.jbuildpilot.builder;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GradleBuildEngine extends AbstractBuildEngine {

    public GradleBuildEngine(PermissionManager permissionManager, ProcessRunner processRunner) {
        super(permissionManager, processRunner);
    }

    @Override
    protected List<String> buildCommand(BuildRequest request) {
        List<String> command = new ArrayList<>();
        
        String executable;
        if (request.isUseWrapper()) {
            executable = isWindows() ? "gradlew.bat" : "./gradlew";
            command.add(request.getProjectDirectory().resolve(executable).toAbsolutePath().toString());
        } else {
            executable = isWindows() ? "gradle.bat" : "gradle";
            Path toolHome = request.getToolHome();
            if (toolHome != null) {
                command.add(toolHome.resolve("bin").resolve(executable).toAbsolutePath().toString());
            } else {
                command.add(executable); // Fallback to system path
            }
        }

        command.addAll(request.getArguments());

        return command;
    }
}
