# JBuildPilot Repository Agent Mode

JBuildPilot isn't just a local developer tool (`pilot doctor`, `pilot build`). It includes a fully autonomous **Repository Agent Mode** powered by the `jbuildpilot-tasks` module. 

Agent Mode is designed to run in CI/CD pipelines (like GitHub Actions) to automatically diagnose out-of-date environments, generate structured maintenance tasks, wait for human approval, and execute changes.

## Architecture: The Task Engine

Unlike local `doctor --fix` which makes immediate changes, Agent Mode uses a discrete **Task Engine**.

1. **Diagnosis**: The `ProjectAnalyzer` inspects the repository.
2. **Task Generation**: The `TaskEngine` converts findings into a `MaintenancePlan` containing discrete `MaintenanceTask` objects.
3. **Risk Assessment**: Every task is assigned a `RiskLevel`.
4. **Approval**: Tasks wait for explicit human approval before being applied.

### Risk Levels

To prevent catastrophic automated rewrites, JBuildPilot classifies every generated task:

* **LOW**: Safe, backwards-compatible updates (e.g., patching a dependency like JUnit 5.10.0 → 5.10.1).
* **MEDIUM**: Minor version bumps that might require minor config tweaks (e.g., Guava 32 → 33).
* **HIGH**: Major structural migrations (e.g., upgrading the `java` compiler requirement from 17 to 21, or Maven 3.x to 4.x).
* **CRITICAL**: Security-sensitive changes or file deletions.

## CLI Commands

The CLI exposes the Agent Mode via the `project` namespace.

### `pilot project diagnose`

Runs a read-only analysis of the current directory and outputs a structured Maintenance Plan.

```bash
$ pilot project diagnose

JBuildPilot Project Diagnosis
────────────────────────────────
Repository: example/project

TASKS
[JB-001] ENVIRONMENT_UPDATE java 17 -> 21 (Risk: HIGH)
[JB-002] DEPENDENCY_UPDATE org.junit.jupiter:junit-jupiter 5.10.0 -> 5.13.0 (Risk: LOW)
[JB-003] DEPENDENCY_UPDATE com.google.guava:guava 32.x -> 33.0.0-jre (Risk: MEDIUM)
```

### `pilot project apply <task-ids>`

Applies specific, approved tasks to the local repository. *(Used heavily by CI wrappers to execute human-approved tasks).*

```bash
$ pilot project apply JB-001 JB-002
```

## API Usage

You can embed the Task Engine directly into your own Java applications:

```java
TaskEngine engine = new TaskEngine();
MaintenancePlan plan = engine.diagnose(Paths.get("."));

for (MaintenanceTask task : plan.getTasks()) {
    if (task.getRisk() == RiskLevel.LOW) {
        task.setApproved(true);
    }
}

// Pass to an executor or the PullRequestManager
```
