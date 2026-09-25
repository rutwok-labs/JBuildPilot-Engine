# JBuildPilot Java API Reference

The JBuildPilot Java API allows you to embed the zero-trust build engine directly into your own applications, IDEs, or automated pipelines. It guarantees the exact same deterministic resolution, environment management, and security boundaries as the CLI.

## Core Architecture

The entry point for embedding the engine is the `io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot` facade. It relies heavily on a **Builder Pattern** for safe instantiation.

### Initialization

```java
import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;
import java.nio.file.Path;

JBuildPilot pilot = JBuildPilot.builder()
    .project(Path.of("/path/to/project"))
    .permissionHandler(new MyCustomPermissionHandler())
    .defaultApprove(false) // Strict zero-trust mode
    .build();
```

---

## Class: `JBuildPilot`

### `analyze()`
```java
public void analyze()
```
Scans the project directory (e.g., parses `pom.xml` or `build.gradle`) in a read-only, static context. Extracts project metadata and a dependency map without invoking arbitrary code.
* **Returns:** A `ProjectModel` containing the project's metadata (name, group, version) and its detected requirements (like Java Version, Maven Version).

### `doctor(boolean fix)`
```java
public void doctor(boolean fix)
```
Diagnoses the host machine to ensure the required toolchains (JDKs, Maven, Gradle) exist in the local sandbox (`~/.jbuildpilot/`).
* **Parameters:**
  * `fix` - If `true`, the engine will attempt to automatically download and install missing dependencies into the sandbox (this will trigger `DOWNLOAD_TOOL` permission requests).

### `resolve()`
```java
public void resolve()
```
Computes the dependency graph, crossing the project's requirements with the host machine's capabilities to build a deterministic execution plan.

### `build()`
```java
public void build()
```
Executes the build process within an isolated wrapper. It applies security rules, drops unsafe environment variables, and executes the compiler. Triggers `EXECUTE_BUILD` and `NETWORK_CONNECT` permission requests.

---

## Security Integration: `PermissionHandler`

JBuildPilot strictly enforces a zero-trust model. Whenever the engine attempts an action that could alter the host system or access the network, it dispatches a `PermissionRequest` to the configured `PermissionHandler`.

To intercept requests (e.g., to show a GUI popup or log them), implement `PermissionHandler`:

```java
import io.github.rutwoklabs.jbuildpilot.core.permission.*;

public class MyCustomPermissionHandler implements PermissionHandler {
    @Override
    public PermissionDecision request(PermissionRequest request) {
        System.out.println("Engine requested: " + request.type());
        System.out.println("Reason: " + request.reason());
        
        // Auto-approve, DENY, or ask the user
        return PermissionDecision.APPROVE; 
    }
}
```

### `PermissionType` Enum

The type of permission being requested. Key types include:
* `DOWNLOAD_TOOL`: Engine wants to download a JDK or build tool.
* `EXECUTE_BUILD`: Engine wants to execute a build script.
* `MODIFY_PATH`: Engine wants to modify system environment variables.
* `NETWORK_CONNECT`: Engine wants to download project dependencies.

### `PermissionRequest` Properties

* `type()`: The `PermissionType`.
* `description()`: A human-readable summary of the action.
* `reason()`: Why the engine needs this permission.
* `toolName()`: The specific tool being requested (if applicable).
* `requiresAdministrator()`: Whether this action requires elevated OS privileges.
