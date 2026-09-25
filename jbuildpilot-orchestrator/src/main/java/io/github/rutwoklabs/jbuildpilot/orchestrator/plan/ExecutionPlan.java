package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

import java.util.List;

public class ExecutionPlan {
    private final String projectName;
    private final List<ExecutionStep> steps;

    public ExecutionPlan(String projectName, List<ExecutionStep> steps) {
        this.projectName = projectName;
        this.steps = steps;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<ExecutionStep> getSteps() {
        return steps;
    }

    public void executeAll(EngineContext context) {
        for (ExecutionStep step : steps) {
            step.execute(context);
        }
    }
}
