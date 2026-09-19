# Harbor Master — Architecture Specification & System Manifest

**Document Version:** 1.0.0  
**Classification:** Public Domain / Spaceport Maritime Cargo Ingress  
**Status:** Approved Architecture Baseline  

---

## 1. Executive Architecture Overview

`harbor-master` is a high-throughput, multi-protocol cargo intake, quarantine, and manifest-validation engine. Designed for mission-critical port authority and orbital cargo docks, it establishes a deterministic gatekeeper perimeter between unverified external transport conduits and trusted internal downstream storage/processing clusters.

Every payload entering the perimeter is treated as untrusted, isolated immediately upon ingress, fingerprinted with cryptographic verification, inspected via zero-trust validation sieves, and routed deterministically.

```
       [ External Transports: SFTP / HTTP Stream / REST Manifest ]
                                   │
                                   ▼
                       ┌──────────────────────┐
                       │   approach-watcher   │◄─── Perimeter Ingress
                       └──────────┬───────────┘
                                  │ Records Intake
                                  ▼
                    ┌────────────────────────────┐
                    │  PostgreSQL: inbound_files │ (Immutable Data Perimeter)
                    └─────────────┬──────────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
       ┌─────────────────────┐        ┌───────────────────────┐
       │ stevedore-extractor │        │  quarantine-validator │
       │ (Stream Decompress) │───────►│  (Schema & Tenant Gate│
       └─────────────────────┘        └───────────┬───────────┘
                                                  │
                                                  ▼
                                      ┌───────────────────────┐
                                      │     signal-tower      │
                                      │ (Dispatch / Telemetry)│
                                      └───────────────────────┘
```

### Core Technology Stack
- **Language & Runtime:** Kotlin 2.x on Java 21 LTS (JVM 21 virtual threads / Project Loom for high-concurrency I/O).
- **Core Persistence:** PostgreSQL 16+ (ACID perimeter ledger, JSONB validation reports, hash index lookups).
- **Architecture Standard:** Uncle Bob Clean Architecture (strict inward dependency rule, immutable domain models, decoupled protocol adapters).
- **Deployment & Orchestration:** Minikube (Local Dev & Test) / Production Kubernetes. Containerized ephemeral workers with bounded memory cgroups.

---

## 2. Service Boundary Breakdown

```
harbor-master/
├── modules/
│   ├── common-domain/          # Core entities, value objects, failure taxonomy, domain events
│   ├── approach-watcher/       # Multi-protocol intake adapters (SFTP, HTTP Streaming, REST)
│   ├── stevedore-extractor/    # Streaming decompression, archive flattening, ZIP64 unpacker
│   ├── quarantine-validator/   # Schema sieve (XSD/JSON), tenant isolation, security gate
│   └── signal-tower/           # Fleet telemetry, operational dispatcher, alerting webhooks
├── deploy/
│   ├── k8s/                    # Kubernetes manifests (Deployments, Services, ConfigMaps)
│   └── minikube/               # Local developer sandbox overlays & mock dependencies
└── docs/
    └── SPEC.md                 # System specification and architecture manual
```

### 2.1 `common-domain`
The foundational, dependency-free core containing immutable domain models, value objects, and deterministic business rules.
- **`InboundFile`**: Root aggregate representing an unverified raw payload arriving at the dock, stamped with a deterministic UUID, SHA-256 fingerprint, byte size, protocol origin, and tenant identity.
- **`CargoManifest` & `ManifestItem`**: Strongly-typed domain representations of bill-of-lading cargo contents, container manifests, and declared itemized cargo specs.
- **`ValidationResult`**: Monadic outcome (`Admitted` vs `Quarantined`) encapsulating zero-or-more validation rule violations, schema errors, or policy breaches.
- **`FailureClassification`**: First-class taxonomy splitting operational disruptions into deterministic categories (see Section 4).
- **`TenantMetadata`**: Tenant identifiers, credential scopes, schema version bindings, and throughput quota allocations.

### 2.2 `approach-watcher`
The perimeter sentinel. Operates non-blocking intake listeners and active pollers across heterogeneous transport protocols:
- **SFTP Inbound Poller**: Secure polling worker inspecting remote drop directories, acquiring lock tokens, streaming remote octets, and performing atomic handoffs.
- **HTTP Chunked Stream Receiver**: Reactive HTTP endpoints accepting streaming binary uploads without holding full payloads in heap memory.
- **REST Manifest Intake**: Synchronous JSON/XML manifest submission endpoints for immediate pre-clearance validation.
- **Perimeter Registrar**: Writes raw incoming drop metadata into the immutable `inbound_files` ledger prior to handoff.

### 2.3 `stevedore-extractor`
The cargo unloader and unpacker. Specializes in archive inspection and decompression under strict resource ceilings:
- **ZIP64 Multi-Part Unpacking**: Robust handling of large compressed archives, nested structures, and multi-part archive volumes.
- **Leaf-File Flattening**: Traverses hierarchical directory structures within archives, flattening payload elements into deterministic canonical paths while neutralizing directory traversal attacks (`../` zip slips).
- **Memory-Safe Streaming Decompression**: Implements constant-memory decompression streams. Never loads entire multi-gigabyte archives into the JVM heap, safeguarding container runtimes against Out-Of-Memory (OOM) termination.
- **Decompression Bomb Defense**: Enforces strict expansion ratio caps (e.g., maximum 100:1 uncompressed-to-compressed size ratio) and aborts malicious bombs instantly.

### 2.4 `quarantine-validator`
The zero-trust security sieve and manifest gatekeeper:
- **Schema Validation Sieve**: Validates payload structures against strict XSD (XML) and JSON Schemas versioned per tenant contract.
- **Tenant Verification**: Asserts cryptographic or header provenance against tenant registry white-lists.
- **Quarantine Isolation Gate**: Payloads that breach schema constraints or contain malformed declarations are immediately locked into quarantine status. Quarantined payloads are isolated from downstream processing pipelines while retaining full raw bytes for forensic analysis.

### 2.5 `signal-tower`
The operational dispatcher, alerting beacon, and telemetry hub:
- **Manifest Dispatcher**: Dispatches verified (`ADMITTED`) cargo manifests to downstream internal harbor routing systems and storage pools.
- **Telemetry & Metrics**: Emits OpenTelemetry metrics, Prometheus scrapable gauges (throughput, ingress latency, quarantine rate), and structured audit logs.
- **Operational Notifier**: Routes alerts according to failure taxonomy: operational system outages to infrastructure alerts, data anomalies to cargo operations webhooks.

---

## 3. The Immutable Data Perimeter (`inbound_files`)

The `inbound_files` table serves as the immutable gatekeeper ledger at the edge of the harbor.

### 3.1 Architectural Rationale
1. **Zero-Trust Ingress Ledger**: Every byte stream touching the harbor perimeter is recorded before downstream processing. If downstream workers fail, crash, or are restarted, the perimeter record remains intact.
2. **Protection Against Downstream Exhaustion**: By asserting SHA-256 idempotency at the perimeter, duplicate or re-delivered large files are deduplicated or referenced immediately without triggering duplicate extraction cycles.
3. **Forensic Audit Provenance**: Stores raw filename, declared tenant, transport protocol, hash, file size, and structured validation diagnostic reports (`JSONB`). This enables non-repudiation and post-incident investigation for corrupted cargo.
4. **Decoupled Pipeline Hand-off**: Downstream workers poll or receive event notifications keyed by immutable `inbound_files.id`, decoupling ingest velocity from extraction and validation velocity.

### 3.2 PostgreSQL Schema Definition

```sql
CREATE TABLE inbound_files (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payload_hash_sha256     CHAR(64) NOT NULL,
    raw_filename            VARCHAR(255) NOT NULL,
    payload_size_bytes      BIGINT NOT NULL CHECK (payload_size_bytes >= 0),
    intake_protocol         VARCHAR(32) NOT NULL,
    tenant_id               VARCHAR(64) NOT NULL,
    quarantine_status       VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    validation_errors       JSONB,
    storage_uri             TEXT,
    received_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_intake_protocol CHECK (
        intake_protocol IN ('SFTP', 'HTTP_STREAM', 'REST', 'MANUAL_DROP')
    ),
    CONSTRAINT chk_quarantine_status CHECK (
        quarantine_status IN ('PENDING', 'ADMITTED', 'QUARANTINED', 'REJECTED')
    )
);

-- Indexing for deduplication and rapid hash lookups
CREATE INDEX idx_inbound_files_hash 
    ON inbound_files (payload_hash_sha256);

-- Indexing for worker polling and quarantine triaging
CREATE INDEX idx_inbound_files_tenant_status 
    ON inbound_files (tenant_id, quarantine_status);

-- Indexing for time-series auditing and cleanup sweeps
CREATE INDEX idx_inbound_files_received_at 
    ON inbound_files (received_at DESC);
```

---

## 4. Binary Failure Taxonomy

To maintain high availability and prevent alert fatigue, system anomalies are strictly bifurcated into two mutually exclusive operational domains:

```
                            [ Ingress Exception ]
                                      │
                   Is the fault in code/infrastructure?
                                ╱          ╲
                            YES              NO
                            ╱                  ╲
                           ▼                    ▼
               ┌──────────────────────┐    ┌──────────────────────┐
               │     System Fault     │    │    Data Exception    │
               │  (Infrastructure/IO) │    │  (Payload/Contract)  │
               └──────────┬───────────┘    └──────────┬───────────┘
                          │                           │
                          ▼                           ▼
                 PagerDuty / SRE Paging       Discord Webhook Alert:
                 Escalation Tier 1            #cargo-ops-exceptions
```

### 4.1 System Faults (Infrastructure Failure)
- **Definition**: Failures caused by system environment, infrastructure degradation, software bugs, or resource starvation where the inbound cargo might be completely valid.
- **Triggers**:
  - Local disk full / ephemeral storage volume exhaustion.
  - PostgreSQL database connection pool starvation or host unreachable.
  - Network I/O timeout during SFTP transport handshake.
  - Worker pod OOM (Out Of Memory) or container crash.
- **Resolution Path**: PagerDuty / SRE on-call rotation. The system enters exponential backoff and leaves the unverified payload in `PENDING` state for automated retry.

### 4.2 Data Exceptions (Cargo Contract Failure)
- **Definition**: The infrastructure is healthy, but the submitted cargo breaches security, validation, schema, or structural constraints.
- **Triggers**:
  - Malformed XML/JSON failing canonical XSD or JSON Schema validation.
  - Corrupted archive (CRC failure, truncated ZIP, nested zip-slip traversal attempt).
  - Unrecognized or unauthorized `tenant_id`.
  - Empty payload or expansion bomb ratio exceeded.
- **Resolution Path**: The payload is stamped with `quarantine_status = 'QUARANTINED'`, validation errors are stored as structured JSONB, and a notification is dispatched to operations:
  - **Destination**: Discord Webhook `#cargo-ops-exceptions`.
  - **Payload**: Tenant ID, Raw Filename, File Size, SHA-256 Fingerprint, Error Diagnostic Summary.
  - **Action**: No engineering pager is alerted; business/cargo operations staff contact the vendor/tenant for re-transmission.

---

## 5. Minikube & Container Topology

### 5.1 Ephemeral Batch Worker Pattern
To prevent unbounded memory growth and cross-tenant resource contamination, archive extraction and schema validation execute inside constrained worker boundaries:
- **Streaming Handlers**: Streaming chunked decoders with max heap configured to `-Xmx512m` per worker pod.
- **Single-File Isolation**: Processing is scoped per individual `inbound_files` record. A corrupted file or memory leak cannot compromise peer workers.
- **Memory Cgroups**: Kubernetes container resource limits set to `limits.memory: 768Mi` and `requests.memory: 256Mi`.

### 5.2 Local Minikube Topology
The local development environment (`deploy/minikube/`) replicates the production cluster:
1. **`harbor-postgres`**: StatefulSet running PostgreSQL 16 with pre-mounted migrations initializing `inbound_files`.
2. **`mock-sftp-dock`**: Internal SFTP server pre-loaded with sample vendor drop boxes and synthetic manifests.
3. **`harbor-master-approach`**: Pod exposing ingress ports for HTTP and polling `mock-sftp-dock`.
4. **`harbor-master-stevedore`**: Worker deployment consuming intake jobs from the perimeter queue.
5. **ConfigMaps & Secrets**:
   - `harbor-config`: Log levels, schema directory paths, expansion caps, webhook endpoints.
   - `harbor-secrets`: Database credentials, SFTP private keys.

---

## 6. Clean-Room Implementation Roadmap

The system will be constructed in five clean-room phases adhering to strict inward dependency rules:

```
Phase 1: common-domain & Perimeter Ledger (PostgreSQL schema & data entities)
    │
    ▼
Phase 2: quarantine-validator (Zero-trust schema sieve & quarantine gates)
    │
    ▼
Phase 3: stevedore-extractor (Memory-bounded streaming unpacker & flattener)
    │
    ▼
Phase 4: approach-watcher (SFTP poller, HTTP stream receiver, REST endpoints)
    │
    ▼
Phase 5: signal-tower & Orchestration (Dispatch, Minikube manifests & telemetry)
```

1. **Phase 1: `common-domain` & Database Ledger**
   - Implement domain entities (`InboundFile`, `CargoManifest`, `ManifestItem`, `ValidationResult`).
   - Define database migrations for `inbound_files` table with appropriate indexes and constraints.
   - Establish unit test fixtures for domain immutability and contract testing.

2. **Phase 2: `quarantine-validator` Schema Sieve**
   - Build XSD and JSON schema validation engine.
   - Implement tenant resolution and schema version matching.
   - Implement `ValidationResult` compiler writing diagnostic error trees into JSONB.

3. **Phase 3: `stevedore-extractor` Streaming Unpacker**
   - Build memory-safe ZIP64 streaming unpacker with zip-slip path sanitization.
   - Implement decompression bomb ratio guards.
   - Implement directory leaf-file flattening into canonical manifest item streams.

4. **Phase 4: `approach-watcher` Ingress Conduits**
   - Build reactive SFTP client with remote locking and atomic drop acquisition.
   - Implement HTTP streaming payload intake with SHA-256 hashing on-the-fly.
   - Connect ingress events to `inbound_files` insertion.

5. **Phase 5: `signal-tower` & Deployment Orchestration**
   - Build manifest dispatch pipeline for admitted payloads.
   - Implement Discord webhook dispatcher for `#cargo-ops-exceptions`.
   - Package Minikube deployment manifests, mock SFTP server, and end-to-end integration tests.
