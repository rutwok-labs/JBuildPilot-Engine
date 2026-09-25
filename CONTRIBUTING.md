# Contributing to JBuildPilot Engine

We welcome contributions! Please see our guidelines below.

## Getting Started
1. Fork the repository
2. Clone locally
3. Ensure JDK 21+ and Maven 3.8+ are installed
4. Build locally, skipping the release-only GPG signing and source/javadoc jars:
   ```bash
   mvn clean install -DskipTests -Dgpg.skip=true -Dmaven.source.skip=true -Dmaven.javadoc.skip=true
   ```

## Pull Requests
- Include tests for new features.
- Ensure all existing tests pass (`mvn clean test`).
- Describe the rationale for your changes clearly.

## Security
If you find a security issue, please do NOT open a public issue. Refer to `SECURITY.md`.
