package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.jbe.model.JbeProgram;

import java.util.ArrayList;
import java.util.List;

public class PlanCompiler {

    public static ExecutionPlan compile(JbeProgram program) {
        List<ExecutionStep> steps = new ArrayList<>();

        if (program.getJavaVersion() != null) {
            steps.add(new RequireJavaStep(program.getJavaVersion()));
        }

        if (program.getBuildSystem() != null) {
            steps.add(new RequireToolStep(program.getBuildSystem()));
        }

        for (String dep : program.getDependencies()) {
            steps.add(new RequireDependencyStep(dep));
        }

        if (program.getBuildSystem() != null) {
            steps.add(new BuildStep(program.getBuildSystem()));
        }

        if (program.getMainClass() != null) {
            steps.add(new RunStep(program.getMainClass(), program.getBuildSystem() != null ? program.getBuildSystem().toLowerCase() : "maven"));
        }

        return new ExecutionPlan(program.getProjectName(), steps);
    }
}
