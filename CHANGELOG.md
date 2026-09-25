# Changelog

All notable changes to the **JBuildPilot Engine** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres strictly to semantic versioning principles.

---

## [v0.1.0-alpha.1] - 2026-09-25

First public (alpha) release on GitHub.

### Release Summary
This release consolidates the foundational engine build (Phases 1 through 14). The engine is now capable of securely scanning a codebase, requesting explicit user permissions, securely acquiring components, orchestrating executions, enforcing rigorous anti-malware constraints, and generating templated projects natively.

### Added
* **Template Registry (`jbuildpilot-template`):** Added a robust Template Registry subsystem allowing developers to bootstrap new projects from predefined boilerplates (`pilot create`).
* **Template Generator Engine:** Safely processes directory trees and substitutes template variables (`PROJECT_NAME`, `PACKAGE_NAME`, etc.) using explicit `{{VARIABLE}}` delimiters in both file contents and file/directory names. `PACKAGE_PATH` is auto-derived from `PACKAGE_NAME`. All resolved paths are validated against traversal before any write.
* **Core Domain (`jbuildpilot-core`):** Established immutable representations for environments, version constraints, and the central Permission model.
* **Static Analyzer (`jbuildpilot-analyzer`):** Delivered zero-execution parsers using secured DOM (for Maven) and Static Regex (for Gradle).
* **Requirement Resolver (`jbuildpilot-resolver`):** DAG builder synthesizing raw requirements into node relationships.
* **Environment Manager (`jbuildpilot-environment`):** Managed the `~/.jbuildpilot/` virtual sandbox with strict zero-trust `PathManager` controllers (blocking `../../`).
* **Secure Downloader (`jbuildpilot-downloader`):** Native Java 11+ `HttpClient` downloader enforcing HTTPS and SHA-256 validation.
* **Security Subsystem (`jbuildpilot-security`):** External API integration for behavioral and static malware scanning.
* **Builder (`jbuildpilot-builder`):** Execution isolation wrapping ProcessBuilders. Added stripping of unsafe JVM environment variables (`LD_PRELOAD`, `JAVA_TOOL_OPTIONS`).
* **JBE Language (`jbuildpilot-jbe`):** Lexer, Parser, and Validator for the proprietary JBuildPilot Execution Language.
* **Orchestrator (`jbuildpilot-orchestrator`):** Execution Planner mapping operations to sequential phases enforcing the permission model.
* **CLI (`jbuildpilot-cli`):** Zero-dependency terminal CLI wrapping the engine with an interactive End-to-End automation pipeline.

### Changed
* **Repository Architecture:** Configured a strict multi-module reactor targeting JDK 21.
* **Security Audits:** Completed comprehensive Threat Modeling and integrated anti-injection defenses into `DefaultProcessRunner` and `PathManager`.
* **Template variable substitution** now requires explicit `{{VARIABLE}}` delimiters. Earlier development builds also attempted to replace *bare* variable names (e.g. a literal `MAIN_CLASS` token) anywhere in a file; that behavior was removed because it silently corrupted unrelated content.

### Fixed (pre-release hardening pass)
* **Build failures now propagate.** The orchestrator previously logged "Execution Plan Completed Successfully" even when the underlying build returned a non-zero exit code, so `pilot build` exited `0` on a broken build. `BuildStep` now fails the plan, `pilot build` returns a non-zero exit code, and a regression test guards this.
* **Version comparison** (`Version`) is now semantic (`1.10 > 1.9`, `1.0 == 1.0.0`, pre-release < release) with a rebuilt `VersionConstraint` supporting exact tokens, wildcards, and Maven-style ranges.
* **Process execution** (`DefaultProcessRunner`) drains stderr on a separate thread to avoid pipe-buffer deadlock, enforces a wait timeout, force-destroys orphaned processes, and restores the interrupt flag.
* **HTTP timeouts** added to the GitHub client, artifact downloader, and MetaDefender scanner (connect + per-request), preventing indefinite hangs.
* **GitHub client** JSON payloads now escape backslashes (in addition to quotes and newlines); the CLI `github` command no longer calls a non-existent no-arg constructor.
* **Host tool detectors** now honor the requested version constraint (returning `INCOMPATIBLE` where appropriate), apply a process timeout, and no longer leak process handles. The Maven installer validation now recognizes the `mvn` launcher.
* **Path-traversal guards** in `TaskEngine` normalize the base directory before comparison; the cache now falls back to a non-atomic replace move when an atomic move across volumes is unsupported.
* **Resource leaks** closed across `Files.walk` streams in the analyzers, template copier, and tool installer.

### Known Limitations
* Automated tests stub the build engine and mock tool acquisition; a real end-to-end build against an installed Maven/Gradle is not yet covered.
* The default template registry path is machine-specific unless overridden via `JBUILDPILOT_TEMPLATES_DIR` / `jbuildpilot.templates.dir`.
* JBE (`*.jbe`) language support and the GitHub Action packaging are early preview.
* Modules `jbuildpilot-resolver`, the JBE parser/plan compiler, and several CLI subcommands have not been exhaustively audited.

