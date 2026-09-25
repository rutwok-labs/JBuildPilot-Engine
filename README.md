# JBuildPilot Engine

**Status:** Alpha (first public release)
**Version:** 0.1.0-alpha.1

> ⚠️ **Alpha maturity notice.** This is the first public release. The engine compiles and its full test suite passes, but the test matrix stubs the external build engine and mocks tool acquisition — an end-to-end run against a *real* Maven/Gradle install has not yet been covered by automated tests. Treat it as a preview: usable for evaluation, not yet hardened for production pipelines. See the [Changelog](CHANGELOG.md) for known limitations.

JBuildPilot Engine is a deterministic, zero-trust build management system. It acts as an isolated translation layer between a repository's declaration of intent (`pom.xml`, `build.gradle`, `*.jbe`) and the physical host environment. It dynamically analyzes projects, requests explicit permissions from the user, acquires missing tooling securely, scans for malware via external APIs, and orchestrates build execution.

## Architecture

The engine is partitioned into the following operational boundaries to prevent security leaks, side effects, and cyclic dependencies:

* **`jbuildpilot-core`**: Immutable domain models, version constraints, and the central Permission model.
* **`jbuildpilot-analyzer`**: Zero-execution AST parsers. Parses build files via secure XML DOMs and static Regex without evaluating groovy/kotlin scripts.
* **`jbuildpilot-resolver`**: Graph builder that synthesizes raw requirements into node-based relationships using strict semantic version intersections.
* **`jbuildpilot-environment`**: Virtual space manager mapping the physical `~/.jbuildpilot/` layout. Features strict anti-traversal path resolution and local host detection.
* **`jbuildpilot-security`**: External API integration layer for behavioral and static malware scanning prior to binary installation (via MetaDefender).
* **`jbuildpilot-downloader`**: Secure artifact acquisition layer enforcing HTTPS, checksum verification, atomic caching, and tight integration with malware scanners.
* **`jbuildpilot-builder`**: Execution isolation layer wrapping ProcessBuilders for Maven and Gradle.
* **`jbuildpilot-jbe`**: Lexer, Parser, and Validator for the proprietary JBuildPilot Execution Language (`*.jbe`).
* **`jbuildpilot-template`**: Project generator capable of initializing new codebases and generating boilerplates.
* **`jbuildpilot-tasks`**: Intelligent maintenance engine representing actionable diagnostic data and risk levels.
* **`jbuildpilot-github`**: Provides embedded interactions for GitHub Actions environments, enabling issue-ops and automated maintenance PRs.
* **`jbuildpilot-orchestrator`**: Execution Planner mapping operations to sequential phases enforcing the permission model.
* **`jbuildpilot-cli`**: Zero-dependency, terminal-native CLI wrapping the core engine with interactive prompts and a complete End-to-End automation pipeline.
* **`jbuildpilot-action`**: External boundary configured as the GitHub Actions execution environment.
* **`jbuildpilot-examples`**: Contains usage demonstrations and examples of engine integration.

## Security Posture

1. **Explicit Authorization**: The engine operates on a strictly default-deny permission model (`PermissionManager`). No network requests, file writes, or binary executions can occur without explicit, context-aware authorization.
2. **Zero Evaluation Analysis**: Projects are analyzed statically. Malicious Gradle plugins or Maven lifecycle hooks cannot trigger during analysis.
3. **Malware Gateway**: Downloaded binaries are held in quarantine while an external malware scanning pipeline evaluates them. Verdicts dictate cache storage.
4. **Path Traversal Mitigation**: All cache writes aggressively sanitize file names. Traversal vectors are mitigated before any `java.nio.file.Path` resolution.

## Installation & Usage

JBuildPilot requires **JDK 21+** and **Maven 3.8+**.

```bash
# Clone the repository
git clone https://github.com/rutwok-labs/JBuildPilot-Engine.git
cd JBuildPilot-Engine

# Build the system across all modules.
# The release profile signs artifacts with GPG and builds source/javadoc jars;
# skip those for a local development build:
mvn clean install -DskipTests -Dgpg.skip=true -Dmaven.source.skip=true -Dmaven.javadoc.skip=true
```

The CLI is mapped via local bootstrap scripts (`pilot` / `pilot.bat`) located in the repository root.

```bash
# 1. Project Creation & Templating
./pilot template          # Manage and search templates
./pilot create            # Initialize a new project from a template
```

> **Template location.** The `template` and `create` commands read from a local template registry. Point the CLI at your registry with the `JBUILDPILOT_TEMPLATES_DIR` environment variable (or the `jbuildpilot.templates.dir` system property); otherwise a built-in default path is used.

```bash
# 2. Analysis & Environment
./pilot analyze           # Analyze project structure and requirements
./pilot doctor            # Check environment health and toolchains
./pilot doctor --fix      # Diagnose and autonomously fix environment issues
./pilot env               # Display isolated Virtual Space paths

# 3. Resolution & Tooling
./pilot resolve           # Compute and resolve missing requirements
./pilot tool <cmd> <pkg>  # Manage internal tools (install/verify)

# 4. Building & Execution
./pilot build             # Autonomously resolve, secure, and build the project
./pilot run               # Autonomously resolve, build, and run the project
./pilot project           # Execute specific project lifecycle tasks

# 5. External Integrations
./pilot github <action>   # Smoke-test internal GitHub Actions embedding (diagnostics only)
```

## Testing

The testing matrix utilizes isolated `@TempDir` structures, synthetic POM files, mocked external APIs, and transient `HttpServer` instances. It does not mutate the physical host machine, nor does it require active public internet or valid API keys to run the test suite.

```bash
mvn clean test
```

## License

See the `LICENSE` file for distribution constraints.
