# Security Policy

## Supported Versions

Currently, only the latest release of JBuildPilot is supported with security updates. 

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x:                |

## Reporting a Vulnerability

We take the security of JBuildPilot very seriously. If you believe you have found a security vulnerability, please report it to us confidentially. 

**Do not open a public issue.**

Instead, please email the security team at `security@rutwok-labs.github.io`.

### What to include

Please provide the following information when reporting a vulnerability:

* A detailed description of the vulnerability and its impact.
* Steps to reproduce the vulnerability, including any necessary payloads or project configurations (e.g., a malicious `pom.xml` or `.jbe` file).
* Information on the environment in which the vulnerability was discovered (OS, Java version, Maven/Gradle versions).
* If applicable, potential remediations or suggestions for fixing the issue.

### Security Model

JBuildPilot is a zero-trust build management system. For an in-depth understanding of our threat model, mitigations, and constraints (including path traversal protections, XXE disabling, and process environment sanitization), please refer to our internal [Security Architecture Documentation](docs/architecture/security.md).
