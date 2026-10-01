# Harbor Master — Architecture Decision Records (ADRs)

This directory documents the architectural decision records for the `harbor-master` maritime cargo ingress and quarantine engine.

## ADR Index

| ADR | Title | Status | Date | Summary |
| :--- | :--- | :--- | :--- | :--- |
| [0001](0001-hybrid-perimeter-kong-oauth-and-kafka-event-backbone.md) | **Hybrid Perimeter Kong OAuth2 Gateway & Kafka Event Backbone** | `APPROVED` | 2026-10-01 | Kong Gateway for perimeter OAuth2 REST ingress; Redpanda/Kafka event backbone for inter-service stage transitions. |
| [0002](0002-modular-docker-compose-with-native-includes.md) | **Modular Docker Compose with Native Includes** | `APPROVED` | 2026-10-01 | Docker Compose v2 top-level `include:` pointing to `deploy/local/services/*.yaml` for granular service debugging in IDE. |
| [0003](0003-hexagonal-storage-port-minio-s3-default-gcs-ready.md) | **Hexagonal Storage Port (MinIO S3 Default, GCS Ready)** | `APPROVED` | 2026-10-01 | Hexagonal `CargoStoragePort` in `common-domain` with MinIO S3 SDK v2 default and GCS adapter compatibility. |
| [0004](0004-postgresql-multi-database-topology.md) | **PostgreSQL Multi-Database Topology** | `APPROVED` | 2026-10-01 | Single PostgreSQL 16 container hosting isolated `harbor_db` and `kong_db` databases for optimal host footprint. |
| [0005](0005-zero-heap-streaming-pipeline-and-quarantine-lifecycle.md) | **Zero-Heap Streaming Pipeline & Quarantine Storage Lifecycle** | `APPROVED` | 2026-10-01 | Zero-heap 64KB bounded streaming buffers, quarantine prefix landing, and server-side pointer flip promotion. |
| [0006](0006-harbor-console-frontend-and-business-exception-triage.md) | **Harbor Console Frontend & Business Exception Triage** | `APPROVED` | 2026-10-01 | Vite + React + TS + Tailwind frontend for Live Cargo Radar and Quarantine Triage Bay (Retry / Override / Reject). |

---

## Architectural Decision Standard

All architectural decisions in this repository follow the clean-room ADR standard:
1. **Context**: Motivation, business/technical drivers, and constraints.
2. **Decision**: Explicit architectural choice and structural implementation.
3. **Consequences**: Positive outcomes and explicit architectural trade-offs.
