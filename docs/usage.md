# JBuildPilot Usage Guide

Welcome to the **JBuildPilot Engine** CLI. JBuildPilot operates on a strict zero-trust model, meaning it analyzes, resolves, and builds Java projects while actively protecting your host machine from malicious build scripts or network calls.

The CLI interacts with the engine through the `pilot` (Linux/macOS) and `pilot.bat` (Windows) scripts.

---

## 1. Core Commands

Navigate to any standard Java project (Maven or Gradle) and run these commands to inspect or build the project safely.

### `pilot analyze`
Scans the current project directory and extracts metadata **without executing any build scripts**.
* **What it does:** Uses secure DOM parsers and static regex to read your `pom.xml` or `build.gradle`, identifying Java requirements, build systems, and dependencies.
* **Usage:** `pilot analyze`

### `pilot doctor`
Diagnoses your host machine's environment.
* **What it does:** Scans for installed JDKs, Maven, Gradle, and system capabilities non-destructively. It alerts you if you lack the required toolchains to build the current project.
* **Usage:** `pilot doctor`

### `pilot resolve`
Computes a mathematical requirement graph.
* **What it does:** Cross-references the project's requirements (from `analyze`) with your host machine's capabilities (from `doctor`) and outputs exactly what needs to be downloaded or configured.
* **Usage:** `pilot resolve`

### `pilot env`
Displays the active Virtual Space paths.
* **What it does:** Shows where JBuildPilot is caching dependencies, downloading secure toolchains, and quarantining files (`~/.jbuildpilot/`).
* **Usage:** `pilot env`

### `pilot build`
Executes the build process in an isolated wrapper.
* **What it does:** Orchestrates the build. If dependencies or JDKs are missing, it asks for permission to securely download them, strips unsafe JVM environment variables, and executes the compiler.
* **Usage:** `pilot build`

### `pilot run`
Executes the compiled artifact.
* **What it does:** Runs your project's main class securely using the resolved environment.
* **Usage:** `pilot run`

---

## 2. Project Generation (Templates)

JBuildPilot features a Template Engine that safely generates new boilerplate projects from a remote or local registry.

### `pilot template list`
Lists all available templates in the registry.

> **Registry location.** By default the CLI looks for the template registry at a built-in path. Override it with the `JBUILDPILOT_TEMPLATES_DIR` environment variable or the `jbuildpilot.templates.dir` system property to point at your own registry.

### `pilot template search <query>`
Searches the template registry for specific keywords (e.g., `pilot template search paper`).

### `pilot template info <template-id>`
Displays detailed requirements and available variables for a specific template.

### `pilot create <template-id> <project-name>`
Generates a new project from a template interactively.
* **Example:** `pilot create paper-plugin-gradle MyNewPlugin`
* **Workflow:** The engine will prompt you for required variables (like `PACKAGE_NAME`, `MAIN_CLASS`). It natively sanitizes paths and safely constructs the project directory.

### Core Capabilities
* **[Repository Agent Mode](agent-mode.md):** Autonomous CI/CD task generation and risk-assessment.
* **[GitHub Integration](github-integration.md):** Pre-built GitHub Action wrappers and Pull Request automation.

---

## 3. How to Use JBuildPilot with Your Own Project

You do not need to alter your existing Java projects to use JBuildPilot. The engine is designed to wrap around standard ecosystems.

### Step 1: Navigate to your project
Open your terminal and `cd` into your existing Maven or Gradle project.

### Step 2: Analyze the project
Run `pilot analyze`. JBuildPilot will automatically detect whether you are using `pom.xml` or `build.gradle` and extract the necessary Java versions (e.g., JDK 21).

### Step 3: Trigger the Build
Run `pilot build`. 
* **The Permission Prompt:** Unlike standard build tools, JBuildPilot will pause and prompt you if it needs to access the network or download a missing JDK.
* *Example Output:*
  ```
  JBuildPilot Permission Request
  ────────────────────────────────
  Action:
    Download missing requirements: 1 items
  Administrator privileges:
    No
  Allow this operation? [y/N]:
  ```
* Type `y` to approve. The engine will acquire the resources securely, verify their SHA-256 checksums, and proceed with the build.

### Advanced: Custom `.jbe` Scripts
If you want to bypass standard Maven/Gradle execution, you can define a `.jbe` (JBuildPilot Execution) file in your project root. JBuildPilot will parse this proprietary language to execute highly specific, isolated build phases. *(Note: JBE language support is currently in early preview).*


---

## 4. Java API (Embedding JBuildPilot)

JBuildPilot can be embedded directly into your own Java applications (e.g., custom IDEs, CI/CD pipelines, or deployment managers). The core facade provides a clean builder pattern that guarantees the exact same zero-trust security model as the CLI.

### Maven Dependency

First, add the orchestrator dependency to your project:

```xml
<dependency>
    <groupId>io.github.rutwok-labs.jbuildpilot</groupId>
    <artifactId>jbuildpilot-orchestrator</artifactId>
    <version>0.1.0-alpha.1</version>
</dependency>
```

### API Usage Example

The API is accessed through `JBuildPilot.builder()`. You can optionally supply your own `PermissionHandler` to pipe zero-trust security prompts directly into your own UI, logging system, or automated rules engine.

```java
import io.github.rutwoklabs.jbuildpilot.orchestrator.JBuildPilot;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import java.nio.file.Path;

public class EmbedDemo {
    public static void main(String[] args) {
        // 1. Initialize the API Facade
        JBuildPilot pilot = JBuildPilot.builder()
                .project(Path.of("/path/to/target/project"))
                
                // Provide a custom PermissionHandler to intercept operations
                .permissionHandler(request -> {
                    System.out.println("[INTERCEPT] Requesting: " + request.description());
                    // In a real application, you might show a GUI popup here.
                    // For this automated pipeline example, we approve it:
                    return PermissionDecision.APPROVE; 
                })
                .build();

        // 2. Perform safe, read-only static analysis (detects Maven/Gradle, extracts metadata)
        pilot.analyze();

        // 3. Run the environment doctor to ensure toolchains exist. 
        // Passing 'true' allows the engine to auto-fix missing dependencies (like JDKs).
        pilot.doctor(true);

        // 4. Resolve the requirement graph
        pilot.resolve();

        // 5. Execute the build process in an isolated wrapper
        pilot.build();
    }
}
```
