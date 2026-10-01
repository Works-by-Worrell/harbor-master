# ADR 0007: HashiCorp Vault for Externalized Secret Management

* **Status:** APPROVED
* **Date:** 2026-10-01
* **Author:** Vector (Intelligence Officer & Nav-Archivist)
* **Deciders:** Commander, LOGOS, Vector

---

## Context

Harbor Master microservices require dynamic access to sensitive infrastructure and partner credentials—including SFTP logins, S3 access keys, database passwords, Berth authentication tokens, and OAuth client secrets—for Berths and Carriers across the perimeter ingress pipeline.

Without externalized secret management, applications risk hardcoding `app.secrets` in configuration files, passing static environment variables, or maintaining in-memory fake mock classes for local testing. This creates security vulnerabilities, causes divergence between local testing and production runtime behavior, prevents zero-downtime credential rotation, and degrades secret governance.

## Decision

We adopt **HashiCorp Vault (KV v2 Secrets Engine)** as the centralized externalized secrets store across all Harbor Master environments:

1. **Centralized KV v2 Secrets Engine**:
   - Store all Berth credentials, carrier authentication keys, transport secrets, and perimeter tokens in HashiCorp Vault under the KV v2 secrets engine (e.g., `secret/data/harbor-master/*`).

2. **Unified Hexagonal Secret Port**:
   - Define a unified `SecretManagerPort` interface in Kotlin `modules/common-domain` that decouples microservices and domain logic from the underlying Vault SDK or implementation.
   - Services resolve credentials dynamically by reference keys (`auth_secret_ref`) at runtime.

3. **Local Dev & Minikube Ergonomics**:
   - Run HashiCorp Vault in developer mode (`vault server -dev`) on port `:8200` with the Vault Web UI enabled for local Docker Compose and Minikube environments.
   - Provide automated bootstrap seeding via initialization scripts (`01-init-secrets.sh`) to pre-populate default development secrets on container startup without manual intervention.

## Consequences

### Positive
- **Zero Application-Level Mock Classes**: Eliminates in-memory fake mock classes; microservices interact with real secret resolution APIs across all environments.
- **Development-to-Production Parity**: Strict parity between local Docker/Minikube developer setups and production cloud Kubernetes clusters.
- **Dynamic Credential Rotation**: Secrets can be rotated dynamically in Vault without restarting microservice containers or redeploying code.
- **Visual Inspection & Auditing**: The Vault Web UI on `http://localhost:8200` enables immediate visual inspection, debugging, and auditability for developers and operators.
- **Clean Architecture Alignment**: Domain logic interacts exclusively with `SecretManagerPort`, maintaining clean hexagonal boundaries.

### Negative / Trade-offs
- **Additional Service Dependency**: Adds HashiCorp Vault container footprint to the local infrastructure compose topology and deployment stack.
- **Authentication & Policy Management**: Requires managing Vault client tokens, AppRole authentication, or Kubernetes service account auth in production environments.
