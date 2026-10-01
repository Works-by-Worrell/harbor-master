# ADR 0001: Hybrid Perimeter Kong OAuth2 Gateway and Kafka Event Backbone

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

The Harbor Master architecture handles high-throughput inbound cargo manifests and payload archives from external shipping carriers across varied transport conduits (REST, HTTP streams, SFTP drops). External ingress requires strict perimeter security, client authentication, rate limiting, and credential management. Conversely, internal inter-service stage transitions (Approach Watcher -> Stevedore Extractor -> Quarantine Validator -> Signal Tower) require high-throughput, low-latency, asynchronous coordination without redundant authentication overhead or direct point-to-point service coupling.

Exposing individual backend service ports directly to the perimeter would create security vulnerabilities, tight coupling, and operational brittleness. Furthermore, forcing internal worker components to authenticate every inter-stage handoff via synchronous HTTP OAuth2 token dances would degrade pipeline throughput, inflate latency, and introduce cascading failure modes.

## Decision

We adopt a **Hybrid Perimeter & Event Backbone Architecture**:

1. **Perimeter Ingress via Kong Gateway (OSS)**:
   - All external client and carrier REST/HTTP interactions terminate at **Kong Gateway (OSS)**.
   - External clients authenticate using the **OAuth2 Client Credentials Grant** (backed by `kong_db` in PostgreSQL).
   - Kong enforces authentication, SSL/TLS termination, rate limiting, request size bounds, and routing to internal ingress handlers (e.g. `approach-watcher`).
   - Internal service ports remain completely isolated from public ingress networks.

2. **Internal Inter-Service Stage Transitions via Kafka / Redpanda Backbone**:
   - Internal pipeline stage transitions are mediated asynchronously via an event backbone using **Redpanda** (Kafka API compatible).
   - Workers consume and produce structured lifecycle events across designated topics (e.g. `harbor.cargo.docked`, `harbor.cargo.extracted`, `harbor.cargo.validated`, `harbor.cargo.admitted`, `harbor.cargo.quarantined`).
   - Stage transitions are decoupled temporally: workers scale independently, absorb traffic spikes without backpressure failures, and operate with zero runtime token dance between trusted internal services.

## Consequences

### Positive
- **Perimeter Isolation**: Zero leakage of internal microservice ports to public networks.
- **High Throughput & Low Latency**: Internal event transitions occur over binary Kafka protocol with zero inter-worker auth overhead.
- **Temporal Decoupling**: Backpressure, burst absorption, and horizontal scaling of worker pods without blocking ingress.
- **Auditable Ingress**: Standardized OAuth2 token issuance and rate limiting managed centrally at the gateway.

### Negative / Trade-offs
- **Infrastructure Dependency**: Requires maintaining Redpanda/Kafka broker instances and Kong Gateway container infrastructure in deployment topologies.
- **Eventual Consistency**: Internal state propagation across asynchronous topics requires idempotent event processing and perimeter ledger reconciliation.
