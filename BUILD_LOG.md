# Harbor Master — Build Log

Engineering milestone and chronological execution log aboard Sovereign Rust.

---

## Milestone 1: Local Deployment Spine & PostgreSQL Database
* **Date:** 2026-10-01
* **Engineer:** Arlo 'Grit' Vance (Chief Engineer) `[grit]`
* **Status:** COMPLETED
* **Architecture References:** ADR-0002 (Modular Docker Compose Native Includes), ADR-0004 (PostgreSQL Multi-Database Topology)

### Summary of Execution
- Established root orchestrator `docker-compose.yaml` using native Docker Compose v2 `include:` specification.
- Provisioned modular PostgreSQL service manifest `deploy/local/services/postgres.yaml` running `postgres:16-alpine` on isolated `harbor-net` bridge network.
- Configured multi-database bootstrap script `deploy/local/bootstrap/postgres/01-init-dbs.sql` to initialize both `harbor_db` (domain perimeter ledger) and `kong_db` (Kong Gateway OAuth2 state) with dedicated role ownerships and permissions.
- Added deterministic container healthcheck (`pg_isready -U harbor_admin -d harbor_db`).
- Validated composition syntax and path resolution via `docker compose config`.

---

## Milestone 2: HashiCorp Vault Infrastructure & Secret Seeding
* **Date:** 2026-10-01
* **Engineer:** Arlo 'Grit' Vance (Chief Engineer) `[grit]`
* **Status:** COMPLETED
* **Architecture References:** ADR-0002 (Modular Docker Compose Native Includes), ADR-0007 (HashiCorp Vault for Externalized Secret Management)

### Summary of Execution
- Provisioned modular HashiCorp Vault service manifest `deploy/local/services/vault.yaml` running `hashicorp/vault:1.16` in development mode on `harbor-net`.
- Configured dev server root token (`harbor-vault-root-token`), IPC_LOCK capability, port mapping `8200:8200`, and deterministic container healthcheck (`vault status`).
- Implemented executable bootstrap script `deploy/local/bootstrap/vault/01-init-secrets.sh` to pre-seed essential berth credentials (`secret/berths/inbound-sftp`, `secret/berths/titan-remote-sftp`) and object storage credentials (`secret/storage/minio`).
- Integrated `deploy/local/services/vault.yaml` into root orchestrator `docker-compose.yaml` include list.
- Validated full compose topology via `docker compose config`.

---

## Milestone 3: Full Local Infrastructure Spine (Redpanda, MinIO, Kong, SFTP)
* **Date:** 2026-10-01
* **Engineer:** Arlo 'Grit' Vance (Chief Engineer) `[grit]`
* **Status:** COMPLETED
* **Architecture References:** ADR-0001 (Hybrid Perimeter Kong OAuth & Kafka Event Backbone), ADR-0002 (Modular Docker Compose Native Includes), ADR-0003 (Hexagonal Storage Port with MinIO S3)

### Summary of Execution
- Provisioned modular Redpanda streaming backbone manifest `deploy/local/services/redpanda.yaml` running `redpanda:v24.1.8` and `console:v2.6.0` with deterministic cluster healthcheck.
- Provisioned modular MinIO S3 object storage manifest `deploy/local/services/minio.yaml` and bucket initialization script `deploy/local/bootstrap/minio/01-init-buckets.sh` establishing `harbor-quarantine` and `harbor-admitted` buckets.
- Provisioned modular Kong Gateway manifest `deploy/local/services/kong.yaml` with automated database migration runner (`kong-migrations`) and OAuth2 bootstrap script `deploy/local/bootstrap/kong/01-setup-oauth.sh`.
- Provisioned modular mock SFTP berth service `deploy/local/services/sftp.yaml` (`atmoz/sftp:latest`) with pre-configured vendor and carrier drop accounts.
- Integrated all service manifests into root orchestrator `docker-compose.yaml`.
- Validated full local deployment spine topology using `docker compose config`.
