# ADR 0004: PostgreSQL Multi-Database Topology

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Harbor Master requires robust, relational persistence for two distinct operational domains:
1. The **Perimeter Ledger & Domain Metadata**: The `docked_payloads` ledger table, carrier contracts, berth configurations, discharge routes, and JSONB inspection reports managed by Harbor Master applications.
2. The **Kong Gateway State**: Routes, services, consumers, credentials, and OAuth2 tokens managed internally by Kong Gateway.

Running separate database container instances for Harbor Master and Kong Gateway would unnecessarily inflate local memory and CPU consumption, complicate deployment topology, and increase operational maintenance overhead. Conversely, mixing Kong's internal schema with Harbor Master's domain ledger inside a single database schema creates namespace pollution and complicates database migrations.

## Decision

We adopt a **Single Container, Multi-Database PostgreSQL 16 Topology**:

1. **Single PostgreSQL 16 Instance**:
   - Run a single PostgreSQL 16 container (`harbor-postgres`).

2. **Isolated Database Engines**:
   - Host two logically separated databases on the same engine instance:
     - `harbor_db`: Dedicated to the Harbor Master perimeter ledger (`docked_payloads`, `berths`, `carriers`, `discharge_routes`) managed via Flyway migrations.
     - `kong_db`: Dedicated exclusively to Kong Gateway metadata, consumer tokens, and OAuth2 state managed via Kong database migrations.

3. **Initialization Automation**:
   - Use container initialization scripts (`/docker-entrypoint-initdb.d/`) to automatically provision both databases and dedicated user roles upon container bootstrapping.

## Consequences

### Positive
- **Minimal Resource Footprint**: Consolidates memory and connection overhead into a single lightweight PostgreSQL container.
- **Clean Schema Separation**: Complete isolation between Harbor Master domain models and Kong internal operational tables.
- **Simplified Local Orchestration**: A single database container dependency for Docker Compose and Minikube setups.
- **Independent Evolution**: Harbor Master Flyway migrations and Kong database migrations operate independently without collision risk.

### Negative / Trade-offs
- **Single Point of Failure in Dev/Local**: In local dev, stopping the PostgreSQL container stops both the application ledger and gateway authentication state. (Production topologies can decouple into managed clusters if needed).
