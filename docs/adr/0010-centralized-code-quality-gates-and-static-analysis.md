# ADR 0010: Centralized Code Quality Gates and Static Analysis

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

To ensure high reliability, security, and maintainability across all Kotlin microservices, libraries, and ingress pipelines in the Sovereign Rust fleet, code quality must be enforced systematically rather than relying on ad-hoc manual code reviews.

Key challenges addressed:
1. **Inconsistent Code Formatting & Kotlin Idioms**: Divergent code styles and anti-patterns degrade readability and complicate cross-project collaboration.
2. **Untracked Security Vulnerabilities & Code Smells**: Complex domain and streaming logic (e.g., streaming I/O, cryptographic verification, quarantine filters) require continuous automated screening for cognitive complexity, resource leaks, and security hotspots.
3. **Test Coverage Erosion**: Without strictly enforced coverage gates, critical perimeter validation paths risk regressing over time without detection.

## Decision

We establish a comprehensive **Two-Tier Code Quality Strategy** combining fast in-process Gradle tooling for local developer feedback with centralized SonarQube quality gates on the LOGOS Platform Hub:

1. **Tier 1: Fast In-Process Gradle Verification (Local & Pre-Commit)**:
   - **`ktlint`**: Enforces strict Kotlin coding conventions and formatting standards with automated lint checks (`./gradlew ktlintCheck`) and formatting tasks (`./gradlew ktlintFormat`).
   - **`detekt`**: Performs static code analysis, identifying Kotlin code smells, complexity spikes, naming anomalies, and potential bugs.
   - **`JaCoCo` (Java Code Coverage)**: Measures line, branch, and instruction coverage during test execution, enforcing a strict minimum threshold of **80% code coverage** via `jacocoTestCoverageVerification` on domain and service modules.

2. **Tier 2: Centralized SonarQube Quality Gates (LOGOS Platform Hub)**:
   - Deploy SonarQube Community/Developer edition permanently on the LOGOS Platform Hub at `https://sonar.worksbyworrell.com`.
   - Integrate the `org.sonarqube` Gradle plugin across all project root builds to ingest JaCoCo XML reports, detekt SARIF results, and test execution metrics during CI runs or scheduled local quality sweeps (`./gradlew sonar`).
   - Enforce hard quality gates in SonarQube: zero new security vulnerabilities, zero high-severity bugs, <3% technical debt ratio, and ≥80% test coverage on new/modified code.

3. **Automated Enforcement & Failure Thresholds**:
   - Quality checks run automatically as part of `./gradlew check` and CI pipeline stages.
   - Build execution halts with a non-zero exit code upon any style violation, detekt defect threshold breach, or JaCoCo coverage failure under 80%.

## Consequences

### Positive
- **Standardized Enterprise Code Hygiene**: Consistent formatting, idiomatic Kotlin practices, and clean architecture boundaries enforced across all fleet repositories.
- **Automated Regression Defense**: Coverage quality gates (80% JaCoCo threshold) prevent untested edge cases and untested domain logic from merging into main branches.
- **Unified Security & Technical Debt Visibility**: Centralized SonarQube dashboards on `https://sonar.worksbyworrell.com` provide persistent historical visibility into security hotspots, complexity trends, and technical debt across all microservices.
- **Immediate Developer Feedback**: In-process Gradle tools deliver sub-second local linting and static analysis without requiring remote network roundtrips.

### Negative / Trade-offs
- **Build Duration Overhead**: Running `ktlint`, `detekt`, `jacocoTestReport`, and `sonar` tasks adds compute time to full verification builds (mitigated by Gradle build cache and running targeted tasks during rapid inner-loop development).
- **Quality Gate Maintenance**: Thresholds, detekt baseline rules, and false-positive exclusions must be actively tuned and curated by lead engineers.
