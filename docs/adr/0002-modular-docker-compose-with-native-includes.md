# ADR 0002: Modular Docker Compose with Native Includes

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Local development ergonomics across the Harbor Master multi-service architecture (PostgreSQL, Redpanda, MinIO, Kong, Approach Watcher, Stevedore Extractor, Quarantine Validator, Signal Tower, Harbor Console) require flexibility. Developers often need to debug a single service in an IDE (e.g., IntelliJ IDEA) with virtual threads and breakpoints while keeping the remaining infrastructure services running in containers.

Monolithic `docker-compose.yml` files become unmaintainable, noisy during code reviews, and cumbersome when developers need to isolate or toggle specific subsets of services. Previous workarounds (like multiple `-f` flags or bash wrapper scripts) introduce complexity and inconsistency across development environments.

## Decision

We adopt the **Docker Compose v2 top-level `include:` directive** with modular service definitions:

1. **Root Composition**:
   - The root `/docker-compose.yaml` acts strictly as an orchestrator using the top-level `include:` directive.
   - Modular service files reside under `deploy/local/services/*.yaml` (e.g., `infra.yaml`, `approach.yaml`, `stevedore.yaml`, `validator.yaml`, `signal-tower.yaml`, `console.yaml`).

2. **File Extension Standardization**:
   - Standardize on `.yaml` file extensions universally across all Compose definitions and deployment manifests.

3. **Granular Developer Control**:
   - Developers can toggle or comment individual service modules in the root `docker-compose.yaml` to spin down specific containerized workers and run/debug them directly from IntelliJ IDEA against the shared local infrastructure.

## Consequences

### Positive
- **Superior Ergonomics**: Clean separation between core infrastructure (PostgreSQL, Redpanda, MinIO, Kong) and application microservices.
- **Selective Debugging**: Developers can run any subset of services in IDEs without modifying complex multi-service configurations or running custom scripts.
- **Maintainability**: Reduced merge conflict risk; isolated service definitions make configuration changes scoped and explicit.
- **Modern Compose Standard**: Leverages native Docker Compose v2 capabilities without external tooling dependencies.

### Negative / Trade-offs
- **Tooling Requirement**: Requires Docker Compose v2.20+ supporting the top-level `include:` specification.
