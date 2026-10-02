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

---

## Milestone 4: S3 Object Storage Migration to LocalStack
* **Date:** 2026-10-01
* **Engineer:** Arlo 'Grit' Vance (Chief Engineer) `[grit]`
* **Status:** COMPLETED
* **Architecture References:** ADR-0002 (Modular Docker Compose Native Includes), ADR-0003 (Hexagonal Storage Port with AWS S3 Emulation)

### Summary of Execution
- Replaced standalone MinIO storage emulator with LocalStack S3 (`localstack/localstack:latest` on port `4566`).
- Provisioned modular LocalStack service manifest `deploy/local/services/localstack.yaml` and initialization hook `deploy/local/bootstrap/localstack/01-init-s3.sh` running in `/etc/localstack/init/ready.d/` to automatically create `harbor-quarantine` and `harbor-admitted` buckets upon readiness.
- Updated HashiCorp Vault secret seeding script `deploy/local/bootstrap/vault/01-init-secrets.sh` to populate `secret/storage/s3` with `endpoint: http://localstack:4566`, `access_key: test`, `secret_key: test`, and `region: us-east-1`.
- Updated root orchestrator `docker-compose.yaml` include definition to swap MinIO for LocalStack.
- Verified live end-to-end container health across all infrastructure services (PostgreSQL, Vault, Redpanda, LocalStack S3, Kong Gateway, SFTP) reporting healthy status.

---

## Milestone 5: Gradle Multi-Module Spine & Common Domain Module
* **Date:** 2026-10-01
* **Engineer:** Arlo 'Grit' Vance (Chief Engineer) `[grit]`
* **Status:** COMPLETED
* **Architecture References:** ADR-0003 (Hexagonal Architecture Ports & Adapters), ADR-0005 (Immutable Docked Payload State Machine), ADR-0006 (Zero-Heap Reactive Streaming)

### Summary of Execution
- Scaffolded root multi-module `build.gradle.kts` with SonarQube, Detekt 1.23.7, Ktlint 12.1.2, Jacoco, and Maven Publishing configured for Forge packages (`packages.worksbyworrell.com`).
- Enforced Java 21 Toolchain, strict nullability compiler options (`-Xjsr305=strict`, `-opt-in=kotlin.RequiresOptIn`), and automated 80.0% minimum branch and line coverage verification across all subprojects.
- Authored clean root `detekt.yml` configuration standardizing code complexity, styling, and naming conventions for Kotlin 2.1.
- Implemented pure Kotlin Hexagonal Domain model in `:modules:common-domain`:
  - **Models & Value Classes:** `BerthId`, `BerthType`, `Berth`, `CarrierCode`, `CarrierStatus`, `CarrierRateLimit`, `Carrier`, `PayloadId`, `QuarantineHash`, `PayloadStatus`, `DockedPayload`, `CargoEnvelope`, `RouteDestination`, `DischargeRoute`.
  - **State Machine Transitions:** Immutable pure transition functions on `DockedPayload` (`quarantine`, `startExtraction`, `startValidation`, `admit`, `reject`, `discharge`) enforcing valid lifecycle progression and rejection paths.
  - **Domain Exceptions:** Typed `HarborException` sealed hierarchy with standard error codes (`QUARANTINE_VIOLATION`, `PAYLOAD_CORRUPTED`, `UNREGISTERED_CARRIER`, `RATE_LIMIT_EXCEEDED`, `BERTH_UNAVAILABLE`, `STORAGE_ERROR`, `INVALID_PAYLOAD_STATE`).
  - **Hexagonal Ports:** Zero-heap streaming storage port (`CargoStoragePort`), Vault credentials port (`SecretManagerPort`), and Kafka event publisher port (`EventPublisherPort`).
- Implemented exhaustive unit test suite achieving 100% test pass rate, 100% branch coverage, and 93.6% line coverage (surpassing the 80.0% threshold).
- Verified complete build, lint, and static analysis suite with zero errors via `./gradlew check jacocoTestCoverageVerification ktlintCheck detekt`.


