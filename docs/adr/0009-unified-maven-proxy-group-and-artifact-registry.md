# ADR 0009: Unified Maven Proxy Group and Hosted Artifact Registry

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Developing and building distributed Kotlin microservices across the Sovereign Rust fleet requires managing two distinct dependency workflows:
1. Resolving external third-party open-source dependencies and plugins (from Maven Central, Gradle Plugin Portal, and Spring/Confluent repositories).
2. Publishing, versioning, and consuming internal first-party shared libraries (e.g., `common-domain`, security protocols, and shared test fixtures) across distinct application repositories.

Without a centralized repository manager:
- Every developer machine and CI worker repeatedly downloads thousands of external JARs over the public internet, consuming bandwidth, degrading build speeds, and introducing vulnerability to external upstream repository outages or rate limiting.
- Sharing first-party code across repositories relies on fragile Git submodules, composite builds, or manual `publishToMavenLocal` steps that fail to scale across team members and remote runners.
- Build configurations become cluttered with multiple disparate repository URLs, fallback mirrors, and authentication blocks.

## Decision

We deploy a centralized, high-performance repository manager (e.g., Gitea / Sonatype Nexus) hosted on the LOGOS Platform Hub to provide a **Unified Maven Proxy Group (`maven-public`)** and hosted private artifact registry:

1. **Unified Virtual Repository Group (`maven-public`)**:
   - Aggregate all internal hosted libraries and external proxy repositories under a single unified Maven repository endpoint:
     `https://repo.worksbyworrell.com/repository/maven-public/`
   - Route resolution order intelligently:
     1. Internal Hosted Releases & Snapshots (`maven-releases`, `maven-snapshots`)
     2. Maven Central Proxy (`maven-central-proxy`)
     3. Gradle Plugin Portal Proxy (`gradle-plugins-proxy`)

2. **Hosted Private Library Publishing**:
   - Provide authenticated hosted repositories for internal artifact releases (`io.worksbyworrell:common-domain:<version>`) using standardized Gradle `maven-publish` tasks.
   - Secure deployment via Vault-injected or environment-configured publishing credentials (`MAVEN_REPO_USER`, `MAVEN_REPO_PASSWORD`).

3. **High-Speed Caching on High-Throughput Storage**:
   - Cache all external dependencies on LOGOS host NVMe storage, ensuring fast local network cache hits.
   - Configure Gradle build settings across all projects (`settings.gradle.kts` / `build.gradle.kts`) to target the single `maven-public` endpoint.

## Consequences

### Positive
- **Sub-Second LAN Dependency Resolution**: External dependencies are downloaded once from upstream and served to subsequent local builds at full gigabit/NVMe line rates.
- **Air-Gap & Upstream Outage Resilience**: Builds remain fully operational even during upstream outages (Maven Central downtime, Cloudflare routing issues, or WAN ISP interruptions) due to persistent local artifact caching.
- **Seamless Shared Library Distribution**: First-party libraries are published once and consumed cleanly via standard Maven coordinates across all fleet microservices.
- **Single Repository Declaration in Gradle**: Eliminates sprawling repository lists in `build.gradle.kts` and `settings.gradle.kts`, standardizing fleet build configurations to a single repository URL.

### Negative / Trade-offs
- **Storage Management on Platform Hub**: Cached artifacts and private releases consume disk space on the LOGOS host, requiring retention policies and periodic cleanup sweeps for stale SNAPSHOT builds.
- **Registry Availability Dependency**: Local builds without pre-cached Gradle dependencies require connectivity to the LOGOS repository proxy for initial resolution.
