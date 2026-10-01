# ADR 0006: Harbor Console Frontend and Business Exception Triage

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

In high-throughput port logistics, cargo intake manifests and archive payloads frequently encounter schema mismatches, corrupted packaging, invalid carrier codes, or unparseable line items. These business and data validation failures are normal operational occurrences rather than engineering system faults.

Without a dedicated operator console, harbor operators and shipping liaison personnel are forced to rely on raw database queries or engineering escalations to inspect failure details, leading to delayed resolution, lack of auditability, and cognitive overload. Operators require direct visibility into real-time cargo movement and dedicated triage capabilities to inspect structured JSONB error reports and execute manual remediation workflows.

## Decision

We establish **`apps/harbor-console` as a Dedicated Operator Interface**:

1. **Frontend Technology Baseline**:
   - Built with **Vite + React + TypeScript + Tailwind CSS** as a responsive, lightweight single-page application.
   - Communicates with Harbor Master management APIs (via Kong Gateway).

2. **Core Operational Views**:
   - **Live Cargo Radar**: Real-time visualization of incoming transport conduits, active berths, and payloads traversing the quarantine lifecycle (`DOCKED` -> `INSPECTING` -> `ADMITTED` / `QUARANTINED`).
   - **Quarantine Triage Bay**: Dedicated workspace for inspecting payloads marked `QUARANTINED`. Displays structured JSONB `inspection_reports`, schema diffs, and validation rule violations.

3. **Operator Remediation Actions**:
   - Enables authorized harbor operators to execute triage actions:
     - **Retry**: Re-queue a quarantined payload for re-evaluation (e.g., following schema/contract updates).
     - **Override**: Force admission with explicit operator audit log and rationale capture.
     - **Reject**: Mark payload as permanently rejected and trigger carrier rejection dispatch.

## Consequences

### Positive
- **Real-Time Operational Control**: Port authorities and operators gain immediate situational awareness over intake berths and pipeline health.
- **Self-Serve Business Exception Triage**: Business and carrier liaison teams resolve data and schema exceptions directly without engineering intervention.
- **Audited Remediation**: Every manual triage override or re-queue is permanently recorded in the perimeter ledger.
- **Clean Separation of Concerns**: Decouples business exception workflows from system infrastructure alerting.

### Negative / Trade-offs
- **Additional Application Footprint**: Introduces frontend build tooling (`npm`/`vite`) and frontend hosting/deployment management.
- **Access Control Overhead**: Requires role-based access control (RBAC) to restrict override and rejection capabilities to authorized operators.
