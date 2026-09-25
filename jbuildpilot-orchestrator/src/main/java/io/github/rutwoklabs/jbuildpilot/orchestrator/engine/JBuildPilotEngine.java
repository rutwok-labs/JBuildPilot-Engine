package io.github.rutwoklabs.jbuildpilot.orchestrator.engine;

import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.ExecutionPlan;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.ExecutionStep;

public class JBuildPilotEngine {

    public void execute(ExecutionPlan plan, EngineContext context) {
        context.log("Starting Execution Plan for Project: " + plan.getProjectName());
        
        for (ExecutionStep step : plan.getSteps()) {
            context.log("-> Step: " + step.getDescription());
            step.execute(context);
        }
        
        context.log("Execution Plan Completed Successfully.");
    }
}
