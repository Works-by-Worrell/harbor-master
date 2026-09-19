# Harbor Master

Harbor Master is a high-throughput, multi-protocol cargo intake, quarantine, and manifest-validation engine designed to secure spaceport and maritime ingress artifacts before admitting them to downstream fleet operations.

Every payload entering the perimeter is treated as untrusted, isolated immediately upon arrival, fingerprinted via SHA-256 cryptographic verification, inspected through deterministic zero-trust validation sieves, and routed accordingly.

For comprehensive architectural design, service boundaries, database schemas, and operational runbooks, see the [Architecture Specification & System Manifest](docs/SPEC.md).

---

## Architectural Vision & Core Principles

- **The Four Pillars of Port Logistics**:
  - **`Berths`**: Transport/connection channels governing protocols, network endpoints, authentication secrets (SFTP, S3, HTTP streams), and connection pooling bounds.
  - **`Carriers`**: Intake sources and shipping partner identities defining drop-zone root paths, intake schedules, and expected manifest schemas.
  - **`DockedPayloads` (`docked_payloads`)**: Immutable perimeter ledger capturing SHA-256 fingerprints, payload byte sizes, raw filenames, arrival timestamps, quarantine status lifecycle (`DOCKED`, `INSPECTING`, `QUARANTINED`, `ADMITTED`), and inspection reports.
  - **`DischargeRoutes`**: Post-quarantine routing destinations governing where verified, extracted cargo is dispatched (target storage buckets, event streams, downstream fulfillment services).
- **Zero-Trust Perimeter Ledger (`docked_payloads`)**: An immutable PostgreSQL perimeter ledger capturing every payload observed at the edge to protect downstream workers from unvetted load, malformed inputs, and malicious drops while guaranteeing audit provenance.
- **Uncle Bob Clean Architecture**: Strict inward dependency flow with decoupled transport protocols, immutable domain models, and swappable infrastructure adapters.
- **Streaming Resource Isolation**: Memory-bounded ZIP64 streaming extraction and validation (`stevedore-extractor`) preventing container Out-Of-Memory (OOM) failures under heavy cargo drops.
- **Deterministic Binary Failure Routing**:
  - *System Faults* (infrastructure, storage exhaustion, database unreachable) route to SRE / infrastructure on-call paging.
  - *Data Exceptions* (schema violations, malformed manifests, corrupted archives) isolate into quarantine and notify cargo operations (`#cargo-ops-exceptions`).

---

## Modules Architecture

```text
harbor-master/
├── modules/
│   ├── common-domain/          # Shared immutable domain models (Berth, Carrier, DockedPayload, DischargeRoute)
│   ├── approach-watcher/       # Multi-protocol ingress adapters (SFTP poller, HTTP stream receiver, REST manifest)
│   ├── stevedore-extractor/    # Memory-safe archive decompression, ZIP64 multi-part unpacking, leaf-file flattening
│   ├── quarantine-validator/   # Security sieve, XSD/JSON schema validation, carrier verification, quarantine gate
│   └── signal-tower/           # Manifest dispatch coordination, OpenTelemetry metrics, operations webhooks
├── deploy/
│   ├── k8s/                    # Production Kubernetes manifests and resource configurations
│   └── minikube/               # Local Minikube development sandbox, PostgreSQL stateful set, SFTP mock
└── docs/
    └── SPEC.md                 # Full Architecture Specification and clean-room system manifest
```

---

## Tech Stack

- **Language & Runtime:** Kotlin 2.x, Java 21 LTS (JVM 21 virtual threads)
- **Persistence:** PostgreSQL 16+ (ACID perimeter ledger & JSONB validation reports)
- **Build Tool:** Gradle 9.x+ with Kotlin DSL (`build.gradle.kts`)
- **Target Deployment:** Minikube (local sandbox) / Production Kubernetes

---

## System Requirements

- **Java Development Kit (JDK)**: 21 (Eclipse Temurin 21 recommended)
- **Gradle**: 9.x+ (bundled wrapper provided via `./gradlew`)
- **Container Runtime**: Docker / Podman / Minikube (for deployment and local integration)

---

## Build & Test

```bash
# Verify project module tree
./gradlew projects

# Compile and run test suite across all modules
./gradlew check
```

---

## Documentation

- [Architecture Specification (`docs/SPEC.md`)](docs/SPEC.md) — Comprehensive technical architecture, PostgreSQL schema design, failure classification taxonomy, container topologies, and clean-room roadmap.
