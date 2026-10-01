# ADR 0003: Hexagonal Storage Port with MinIO S3 Default and GCS Compatibility

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
   - Use **MinIO** as the default storage engine for local development and integration tests, complete with MinIO Web Console enabled on port `9001` for direct visual inspection of quarantine and admitted buckets.

3. **GCS Adapter Compatibility**:
   - Maintain the structural interface contracts such that a `GcsCargoStorageAdapter` (using `com.google.cloud:google-cloud-storage`) can be plugged in without modifying any domain or application logic.

## Consequences

### Positive
- **Dual-Cloud Mastery**: Seamless portability across AWS S3, MinIO, and Google Cloud Storage.
- **Local Developer Experience**: Instant visual inspection of bucket contents, quarantine trees, and admitted manifests via the MinIO Web Console (`localhost:9001`).
- **Clean Architecture Adherence**: Storage technology details remain outside the domain boundary; domain and pipeline stages depend only on `CargoStoragePort`.
- **Streaming Efficiency**: Native support for zero-heap streaming and server-side object promotion across storage backends.

### Negative / Trade-offs
- **Adapter Maintenance**: Requires maintaining and testing adapter implementations against both AWS S3/MinIO and GCS SDKs.
