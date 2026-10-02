# ADR 0003: Hexagonal Storage Port with S3 Default (LocalStack Local Engine) and GCS Compatibility

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Harbor Master requires object storage for managing inbound raw cargo, quarantine isolation prefixes, extracted manifest artifacts, and admitted cargo storage. The system must operate seamlessly across local development, on-premise spaceport installations, and cloud provider deployments (AWS S3, Google Cloud Storage).

Hardcoding provider-specific storage SDKs into domain or worker modules couples the core architecture to a single vendor, complicates local testing, and prevents deployment flexibility.

## Decision

We establish a **Hexagonal Storage Architecture** anchored by a domain port:

1. **Hexagonal Port Definition**:
   - Define `CargoStoragePort` in `modules/common-domain` encapsulating object operations: streaming byte writes, streaming byte reads, prefix listing, server-side object copy/move, metadata retrieval, and zero-CPU native digest extraction.

2. **Default Local & S3 Adapter**:
   - Implement `S3CargoStorageAdapter` using the AWS Java SDK v2 (`software.amazon.awssdk:s3`).
   - Use **LocalStack S3** on port `4566` as the active local S3 engine for local development and integration tests, providing faithful AWS S3 API semantics and container initialization hooks for `harbor-quarantine` and `harbor-admitted` buckets.
   - Maintain compatibility with standard S3-compliant endpoints including MinIO and AWS production S3.

3. **GCS Adapter Compatibility**:
   - Maintain the structural interface contracts such that a `GcsCargoStorageAdapter` (using `com.google.cloud:google-cloud-storage`) can be plugged in without modifying any domain or application logic.

## Consequences

### Positive
- **Cloud Fidelity & Portability**: Seamless portability across AWS S3, LocalStack S3 emulation, and Google Cloud Storage.
- **Local S3 Emulation**: Direct validation of AWS Java SDK v2 client configurations against LocalStack on port `4566` without cloud egress costs.
- **Clean Architecture Adherence**: Storage technology details remain outside the domain boundary; domain and pipeline stages depend only on `CargoStoragePort`.
- **Streaming Efficiency**: Native support for zero-heap streaming and server-side object promotion across storage backends.

### Negative / Trade-offs
- **Adapter Maintenance**: Requires maintaining and testing adapter implementations against both AWS S3/LocalStack and GCS SDKs.
