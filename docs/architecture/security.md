# JBuildPilot Security Model

JBuildPilot is designed as a zero-trust build management system. The core philosophy is that project files (`pom.xml`, `build.gradle`, `*.jbe`) are inherently untrusted input. The engine acts as an isolation layer between the untrusted intent and the host operating system.

## Threat Model

### 1. Malicious Project Files (XXE, Code Execution)
- **Vector**: A malicious `pom.xml` could contain XXE payloads, or a `build.gradle` could contain executing scripts to run when parsed.
- **Mitigation**: JBuildPilot parses build files purely statically. The `MavenAnalyzer` configures the XML `DocumentBuilder` to strictly disable `ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_SCHEMA`, and enables `FEATURE_SECURE_PROCESSING`. `GradleAnalyzer` utilizes standard Regex without ever invoking Groovy or Kotlin scripting engines.

### 2. Path Traversal & Symlink Attacks
- **Vector**: A maliciously crafted dependency ID or build configuration requesting cache extraction outside of the protected `~/.jbuildpilot/` virtual space (e.g., `../../etc/passwd`).
- **Mitigation**: 
  - `PathManager.resolveSafely()` strictly checks that any dynamically resolved child path `startsWith` the normalized absolute base directory, throwing a `SecurityException` otherwise. It also defends against absolute path injections.
  - `Artifact.generateSafeFileName()` aggressively coerces untrusted resource identities to alphanumeric, hyphens, and underscores, stripping out backslashes and periods before caching, rendering `../` injections impossible.

### 3. Command Injection & Unsafe Environment Variables
- **Vector**: Shell concatenation attacks or exploiting process environment inheritance to inject malicious libraries (e.g., `LD_PRELOAD`, `JAVA_TOOL_OPTIONS`).
- **Mitigation**: 
  - JBuildPilot explicitly utilizes `ProcessBuilder` with argument lists `List<String> command`, averting any shell interpretation (`&&`, `;`, `|`).
  - `DefaultProcessRunner` intercepts and explicitly strips common exploitation vectors from the child process environment, including `LD_PRELOAD`, `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS`, `MAVEN_OPTS`, and `GRADLE_OPTS`.

### 4. Malicious Downloaded Artifacts & Supply Chain Attacks
- **Vector**: Compromised repositories or Man-in-the-Middle (MITM) attacks altering binaries in transit.
- **Mitigation**: 
  - `Downloader` explicitly forces HTTPS for all external network requests.
  - Downloaded resources are evaluated via SHA-256 in the `ChecksumVerifier`.
  - All files traverse the `MalwareScanner` boundary (via MetaDefender v4 API). A `MALICIOUS` or `UNKNOWN` verdict denies caching.
  - Approved temporary files are transitioned to the persistent cache using `StandardCopyOption.ATOMIC_MOVE` where the platform supports it, to mitigate Time-of-Check to Time-of-Use (TOCTOU) races. When an atomic move is not possible (e.g. the temp directory and cache live on different volumes), the engine falls back to a replace move.

### 5. Arbitrary Process Execution
- **Vector**: A `.jbe` file attempts to silently execute harmful instructions without the user's consent.
- **Mitigation**: All side-effect generating actions run through the `PermissionManager`. Operations such as `DOWNLOAD`, `EXECUTE_BUILD`, and `FILESYSTEM_ACCESS` must be explicitly granted by the user via interactive terminal prompts or configured CI policies. Execution plans are halted entirely if permission is denied.
