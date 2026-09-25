package io.github.rutwoklabs.jbuildpilot.orchestrator.plan;

import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;

public interface ExecutionStep {
    String getDescription();
    void execute(EngineContext context);
}
