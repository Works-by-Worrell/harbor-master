# ADR 0008: Externalized Shared Platform Hub Architecture

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

As the Sovereign Rust fleet expands with multiple Kotlin microservices, libraries, and auxiliary tools (including `harbor-master`, `example-service`, and shared common domains), embedding shared platform infrastructure—such as artifact package registries, continuous integration agents, SonarQube static analysis engines, and centralized secret management (Vault)—directly into individual application runtime `docker-compose.yaml` manifests creates substantial operational friction.

Embedding full platform suites into local project compose files causes:
1. **Severe Resource Bloat & Startup Latency**: Local developers and CI agents must spin up multiple gigabytes of heavy platform containers (JVM-based SonarQube, package registries, database backends) alongside application containers, slowing startup times to minutes and draining host RAM/CPU.
2. **Configuration Drift & Fragmented State**: Ephemeral local platform containers do not share build artifacts, cached dependencies, or historical telemetry across projects or peer development machines (workstations and laptops).
3. **Redundant Architectural Overhead**: Each microservice repository is burdened with maintaining boilerplate compose services, volume mounts, and network bridges for platform tools.

## Decision

We decouple all shared platform infrastructure from individual application repositories and establish a dedicated, permanent **Platform Hub** hosted on the centralized LOGOS host infrastructure:

1. **Centralized Platform Topology behind Nginx SSL**:
   - Platform infrastructure services (Gitea/Nexus Package Registry, SonarQube, Vault, Woodpecker CI) run permanently on the dedicated LOGOS host machine.
   - All platform endpoints are exposed securely via a centralized Nginx reverse proxy with SSL termination under the `*.worksbyworrell.com` wildcard domain (e.g., `https://repo.worksbyworrell.com`, `https://sonar.worksbyworrell.com`, `https://vault.worksbyworrell.com`).

2. **External Remote Platform Model for Application Services**:
   - Application repositories (such as `harbor-master`) treat all platform infrastructure as external remotes and do not embed registry, SonarQube, or CI runner definitions into their runtime `docker-compose.yaml` manifests.
   - Local application compose configurations focus strictly on core application dependencies (e.g., PostgreSQL, Redpanda/Kafka, MinIO) required for runtime service execution.

3. **Universal Access across Workstations & Laptops**:
   - Development workstations, mobile laptops, and CI build runners connect to the LOGOS Platform Hub via standardized HTTPS endpoints with centralized API tokens and credentials.

## Consequences

### Positive
- **Ultra-Lean Application Manifests**: Application compose stacks start in under 5 seconds (<5s startup) with minimal memory footprint, since heavy platform tools run externally on dedicated hardware.
- **Shared Library Reuse Across Fleet**: Published artifacts and common libraries are instantly accessible to all fleet projects from a single centralized repository.
- **Unified Telemetry & Historical Trends**: Continuous code quality metrics, security scans, and test reports accumulate in a single persistent dashboard rather than getting destroyed when local compose stacks spin down.
- **Consistent Developer Ergonomics**: Seamless transitions between local laptops, dev workstations, and remote build runners using identical remote endpoints.

### Negative / Trade-offs
- **Network Dependency for Platform Operations**: Pushing artifacts or submitting SonarQube reports requires network connectivity to the LOGOS host (mitigated by local Gradle build tool caching and proxy caching).
- **Centralized Administration Overhead**: The LOGOS host reverse proxy, SSL certificates, and platform container lifecycles must be administered and backed up as critical shared infrastructure.
