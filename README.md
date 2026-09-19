# Harbor Master

Harbor Master is an automated inspection, intake, quarantine, and dispatch system designed to secure containerized workloads and ingress artifacts before admitting them to the fleet.

## Modules Architecture

```text
harbor-master/
├── modules/
│   ├── common-domain/          # Shared domain models, value objects, and deterministic contracts
│   ├── approach-watcher/       # Ingress radar, approach sensor, and incoming artifact detection
│   ├── stevedore-extractor/    # Unpacking, payload extraction, and manifest parsing
│   ├── quarantine-validator/   # Policy enforcement, vulnerability inspection, and quarantine gates
│   └── signal-tower/           # Dispatch coordination, telemetry broadcast, and fleet alerts
├── deploy/
│   ├── k8s/                    # Production Kubernetes manifests and Helm charts
│   └── minikube/               # Local Minikube environment configuration and dev overlays
└── docs/                       # Architecture decision records (ADRs) and system specifications
```

## System Requirements

- **Java Development Kit (JDK)**: 21 (Eclipse Temurin 21 recommended)
- **Gradle**: 9.x+ (bundled wrapper provided via `./gradlew`)
- **Container Runtime**: Docker / Podman / Minikube (for deployment and local integration)

## Build & Test

```bash
# Check all projects and tasks
./gradlew projects

# Compile and run test suite across all modules
./gradlew check
```
