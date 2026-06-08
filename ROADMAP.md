# Token Launchpad Platform Roadmap

This roadmap is written in a build-in-public style. It is both an implementation plan and a learning journal for building a Spring Boot microservices platform around token launch campaigns, transactions, asynchronous saga coordination, and cloud deployment.

The goal is not to collect buzzwords. The goal is to add microservice patterns when the project creates a real reason to need them.

## Project Vision

Token Launchpad Platform is a backend platform where users can authenticate, create transactions, reserve campaign tokens, and complete a distributed transaction flow across independent services.

The project is being built to learn:

- Spring Boot microservices
- API Gateway and JWT security
- RabbitMQ asynchronous messaging
- Saga orchestration through events
- Transactional outbox
- Idempotency
- Testcontainers integration tests
- CI/CD
- Docker, AWS, Kubernetes, Prometheus, and Grafana

## Current System

### Services

- `gateway-service`: public entry point, route filtering, JWT validation, user header propagation.
- `sso-service`: registration, login, JWT issuing, user persistence.
- `campaign-service`: campaign lifecycle, token reservation, Redis caching, RabbitMQ consumers, campaign-side outbox.
- `transaction-service`: transaction creation, saga start, transaction-side outbox, saga reply handling.
- `launchpad-common`: shared event contracts used by messaging flows.

### Infrastructure

- PostgreSQL for service persistence.
- Redis for campaign caching.
- RabbitMQ for asynchronous saga messaging.
- Docker Compose for local infrastructure.
- Maven root reactor for multi-module builds.

## What Is Already Implemented

- Basic service split across gateway, auth, campaign, transaction, and shared contracts.
- JWT-based authentication through `sso-service`.
- Gateway-level authentication filter.
- Transaction creation flow.
- Campaign token reservation flow.
- RabbitMQ event flow:
  - transaction-service publishes reserve-token command
  - campaign-service consumes reservation command
  - campaign-service publishes success/failure reply
  - transaction-service consumes saga reply
- Transactional outbox in `transaction-service`.
- Campaign-side reservation idempotency.
- Campaign-side outbox for saga replies.
- Redis cache for campaign reads.
- Testcontainers integration tests for campaign messaging, caching, concurrency, and idempotency.
- Flyway migrations with Hibernate schema validation for production-like configs.
- GitHub Actions CI for the Maven reactor.
- Service Dockerfiles.
- First-pass Kubernetes base manifests for local image deployment.
- Initial roadmap for CD, AWS, Kubernetes, and observability.

## Learning Principles

- Build small vertical slices instead of isolated technical demos.
- Every distributed operation should be retry-safe.
- Every message consumer should be idempotent.
- Every database schema change should be reproducible.
- Every async process should be observable.
- Deployment should come after correctness, not before it.

## Pattern Map

### Already Practicing

| Pattern | Current Usage |
| --- | --- |
| API Gateway | `gateway-service` validates JWT and forwards traffic |
| Database per Service | each domain service owns its persistence model |
| Saga | transaction reservation flow across transaction and campaign services |
| Transactional Outbox | outgoing RabbitMQ messages persisted before publishing |
| Idempotent Consumer | campaign reservation keyed by transaction ID |
| Shared Contracts | event DTOs in `launchpad-common` |
| Cache Aside | campaign cache with Redis and cache eviction |
| Integration Testing | Testcontainers for real PostgreSQL, RabbitMQ, and Redis |

### Next Patterns to Add

| Pattern | Why It Matters Here |
| --- | --- |
| Global Exception Handling | make error responses consistent across every service |
| Request Idempotency | protect public POST requests from duplicate client retries |
| Saga State Machine | make transaction progress explicit and safe under duplicate replies |
| Outbox Row Claiming | allow multiple service instances without duplicate publishing |
| Dead Letter Queues | isolate poison messages instead of retrying forever |
| Correlation IDs | trace one user request across gateway, services, and RabbitMQ |
| Contract Versioning | evolve events without breaking other services |
| CD Pipeline | build and publish deployable images only after CI passes |
| Observability | monitor saga failures, outbox lag, queue depth, and latency |

## Build Milestones

### Milestone 1: Local Microservice Baseline

Status: in progress

Goal: make the platform easy to run, inspect, and test locally.

Work:

- Keep Docker Compose infrastructure healthy.
- Keep root Maven reactor working.
- Document required environment variables.
- Make local service startup predictable.
- Keep integration tests isolated from each other.

Done when:

- A fresh clone can run the core test suite.
- Local infrastructure starts with one Docker Compose command.
- Service configuration is documented.

### Milestone 2: Correctness Before Scale

Status: paused after first pass

Goal: make the distributed transaction flow safe under duplicate messages, retries, partial failures, and concurrent requests.

Work:

- Keep current request idempotency in `transaction-service`.
- Keep idempotency keys scoped by user.
- Keep same-key/different-payload conflict handling.
- Keep transaction creation and outbox event creation in the same database transaction.
- Return to deeper idempotency and saga-state hardening when a concrete duplicate-message bug appears.

Done when:

- Duplicate HTTP transaction requests create one transaction.
- Duplicate RabbitMQ messages do not change state twice.
- A concrete idempotency or saga bug can be reproduced with a failing test before adding more abstractions.

### Milestone 3: Database Migrations

Status: complete

Goal: make schema changes explicit and repeatable.

Work:

- Add Flyway or Liquibase to `sso-service`.
- Add migrations for users and roles.
- Add Flyway or Liquibase to `transaction-service`.
- Add migrations for transactions and outbox events.
- Add Flyway or Liquibase to `campaign-service`.
- Add migrations for campaigns, reservations, and outbox events.
- Switch production-like configs from `ddl-auto: update` to `ddl-auto: validate`.

Done when:

- Databases can be created from migrations only.
- Tests run against migrated schemas.
- Hibernate validates schema instead of generating it in production-like profiles.

### Milestone 4: CI With GitHub Actions

Status: complete, with polish remaining

Goal: validate the platform automatically on every push.

Work:

- Keep `.github/workflows/ci.yml` running the Maven reactor.
- Keep Java 21 and Maven dependency caching.
- Confirm the workflow triggers cover the active development branches.
- Decide whether CI should rely only on Testcontainers or keep GitHub service containers for smoke coverage.
- Make CI logs and failure artifacts useful for debugging.
- Upload test reports on failure.
- Keep CI separate from CD so test validation stays fast and reliable.

Done when:

- Every push runs the test suite.
- Integration tests run in the cloud.
- Failures expose useful logs.

### Milestone 5: Docker Images and CD Pipeline

Status: first pass started

Goal: build and publish versioned service images.

Work:

- Keep Dockerfiles for each service.
- Standardize local Kubernetes image tags across all services.
- Add GitHub Actions CD workflow on a separate branch.
- Build images only after CI passes.
- Push images to DockerHub first or AWS ECR later.
- Tag images with branch and commit SHA.
- Keep registry credentials in GitHub Actions secrets, not in Git.
- Add a root `Jenkinsfile`.
- Optionally add image scanning.

Done when:

- CD builds all service images from a clean checkout.
- Images are traceable to Git commits.
- Registry credentials are not committed.
- The image tags used by Kubernetes manifests are produced by the pipeline.

### Milestone 6: AWS and Linux Provisioning

Status: planned

Goal: move from laptop-only execution to secure Linux infrastructure.

Work:

- Provision EC2 instances.
- Configure SSH key access.
- Disable password login and root login.
- Restrict inbound ports.
- Keep PostgreSQL, Redis, and RabbitMQ internal.
- Store secrets in AWS SSM Parameter Store or Secrets Manager.

Done when:

- Services can run on Linux.
- Only gateway/API entry points are public.
- Secrets are managed outside Git.

### Milestone 7: Kubernetes Orchestration

Status: first pass started

Goal: deploy the platform to Minikube first, then AWS EKS, with publisher behavior ready for multiple service replicas.

Work:

- Keep the existing `k8s/base` Kustomize structure.
- Keep namespace, ConfigMap, Secret references, infra manifests, app Deployments, Services, and probes.
- Replace the committed local-development Secret with a safe template before any public push.
- Decide whether to keep a dev-only Secret manifest ignored locally, use generated secrets, or move secrets to an external provider later.
- Harden outbox publishers in transaction and campaign services before running multiple replicas.
- Add atomic outbox row claiming so two instances cannot publish the same row.
- Recover stuck `PROCESSING` outbox rows.
- Add RabbitMQ DLQs for saga queues.
- Add Minikube overlay.
- Add EKS overlay.
- Consider Helm or Kustomize after raw manifests work.

Done when:

- Outbox rows cannot be published by two service instances at the same time.
- Poison messages are moved to DLQ.
- Platform runs in Minikube.
- Gateway reaches backend services inside the cluster.
- The same base manifests can be adapted for EKS.

### Milestone 8: Observability

Status: planned

Goal: understand system behavior without reading logs manually.

Work:

- Add Actuator and Prometheus metrics to all runtime services.
- Expose `/actuator/prometheus`.
- Add custom saga metrics.
- Add custom outbox metrics.
- Add idempotency metrics.
- Monitor RabbitMQ queue depth and DLQ count.
- Add Grafana dashboards.
- Add alerts for high latency, high error rate, saga failures, and outbox backlog.

Done when:

- Prometheus scrapes all services.
- Grafana shows API latency, saga health, queue depth, and JVM health.
- Outbox backlog and saga failures are visible.

### Milestone 9: Production Hardening

Status: later

Goal: reduce operational and security risks.

Work:

- Add global exception handlers to every runtime service.
- Standardize API error response shape, validation errors, auth failures, and domain exceptions.
- Add structured JSON logging.
- Add correlation IDs across HTTP and RabbitMQ.
- Add resource limits in Docker/Kubernetes.
- Add rolling update strategy.
- Add PostgreSQL backup plan.
- Add secret rotation process.
- Add vulnerability scanning.
- Add environment-specific profiles.

Done when:

- Deployments can roll without full downtime.
- Logs, metrics, and traces can be correlated.
- Runtime secrets are never stored in Git.

## Immediate Cleanup Notes

- The Kubernetes Secret currently contains local development values that are already in local commit history. Do not push that branch as-is to a public remote.
- Before publishing Kubernetes work, replace committed secret values with a template such as `secrets.example.yaml`, keep the real local secret ignored, and rotate any value that has ever been pushed outside the machine.
- If the local commits must be pushed later, clean the branch history first or recreate the branch from a safe base and reapply only non-secret changes.

## Current Focus

The current focus is moving from CI and first-pass Kubernetes manifests toward deployable images and safer service behavior:

- Keep the Maven reactor green on every relevant push and pull request.
- Add global exception handlers consistently across gateway, SSO, campaign, and transaction services.
- Standardize local image tags used by `k8s/base`.
- Create the GitHub Actions CD workflow on a separate branch.
- Decide how to handle the already committed local Kubernetes Secret before publishing any Kubernetes branch.
- Keep migration-backed tests green.

Deferred correctness notes:

- Request idempotency stays on the roadmap, but deeper changes should wait for a reproduced bug or failing test.
- Outbox publisher hardening is scheduled near the Kubernetes milestone, where multiple replicas make duplicate publishing a real operational risk.

## Short-Term Backlog

- Add global exception handlers to every runtime service.
- Standardize Kubernetes image tags.
- Add GitHub Actions CD workflow in a separate branch.
- Replace committed k8s Secret values with a safe template before any public push.
- Add correlation IDs and event IDs.

## Long-Term Backlog

- GitHub Actions or Jenkins image publishing pipeline.
- AWS EC2 provisioning.
- Harden outbox row claiming before multi-replica service deployments.
- Add stuck outbox recovery.
- Add RabbitMQ DLQs.
- Revisit request idempotency and saga reply idempotency when a concrete duplicate-message bug is reproduced.
- Kubernetes Minikube and EKS overlays.
- Prometheus and Grafana dashboards.
- Structured logging.
- Contract versioning with schema documentation.
- Compensation flow for releasing reserved tokens.
- Optional CQRS read model for dashboards.

## Patterns Intentionally Delayed

- CQRS: useful later for read-heavy views, not needed yet.
- Event Sourcing: educational but too heavy for the current stage.
- Service Mesh: better after Kubernetes is stable.
- Workflow engines: worth learning after the RabbitMQ saga is solid.
- More microservices: current services should be hardened first.
