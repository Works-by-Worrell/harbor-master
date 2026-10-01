# ADR 0005: Zero-Heap Streaming Pipeline and Quarantine Storage Lifecycle

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Port and orbital dock cargo intakes involve variable-sized archives ranging from small megabyte manifests to multi-gigabyte cargo drops. Buffering incoming octets or uncompressed archive payloads in JVM heap memory creates severe Out-Of-Memory (OOM) risks, triggers long garbage collection pauses, and destabilizes worker nodes under high concurrency.

Additionally, admitting unverified payloads directly into production data lakes or downstream processing queues violates zero-trust security principles. Redundant byte copying between ingress, inspection, and admission stages wastes network bandwidth and storage I/O.

## Decision

We enforce a **Zero-Heap Streaming Architecture & Ephemeral Quarantine Storage Lifecycle**:

1. **Zero-Heap Buffering Invariant**:
   - Application workers **NEVER** buffer raw payload octets or uncompressed contents into JVM heap.
   - All I/O across pipeline stages (Approach Watcher -> Stevedore Extractor -> Quarantine Validator -> Signal Tower) operates via small, bounded streaming buffers (e.g. 64KB chunks).
   - JVM heap is capped at `-Xmx512m` within container memory cgroups (`768Mi` limit, `256Mi` request). Memory footprint remains constant regardless of payload size (50MB vs. 50GB).

2. **Quarantine Storage Lifecycle**:
   - **Ephemeral Ingress Isolation**: All raw unvetted cargo lands directly in an isolated quarantine prefix (`quarantine/{payload_id}/...`) in object storage. Downstream business consumers have zero access to this prefix.
   - **Streaming In-Flight Extraction**: Stevedore Extractor streams archive decompression directly to storage or bounded scratch volumes without full-archive disk re-buffering.
   - **Instantaneous Server-Side Promotion**: Upon successful inspection and validation, cargo is promoted from quarantine to admitted storage (`admitted/{payload_id}/...`) via an instantaneous server-side pointer flip / copy-free move (e.g. S3 copy/move or GCS rewrite), eliminating cross-network byte copies.

3. **Zero-CPU Native Digest Capture**:
   - Content verification digests (`content_digest`) are captured directly from storage adapter metadata (e.g. CRC32C / MD5 / ETag) upon stream completion at zero extra application CPU cost.

## Consequences

### Positive
- **Payload-Size Invariance**: Deterministic resource utilization; 50GB files process within 512MB RAM with zero OOM risk.
- **Zero-Trust Perimeter**: Complete containment of untrusted payloads until explicit validation approval.
- **I/O & Bandwidth Efficiency**: Server-side promotion avoids costly redundant byte transmission over the network.
- **Predictable GC Characteristics**: Absence of large heap byte arrays ensures near-zero garbage collection pause times.

### Negative / Trade-offs
- **Streaming Code Complexity**: Requires disciplined use of streaming APIs (`InputStream`, `Channels`, reactive byte publishers) and strict resource cleanup.
- **Storage Provider Feature Dependency**: Server-side object moves require supported object storage APIs (standard across S3/MinIO/GCS).
