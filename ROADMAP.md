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
- Basic Minikube deployment smoke test with gateway-to-service flow.
- Global exception handlers for the runtime services, with reactive gateway auth errors handled in the gateway filter.
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
| Global Exception Handling | make error responses consistent across every service |

### Next Patterns to Add

| Pattern | Why It Matters Here |
| --- | --- |
| Request Idempotency | protect public POST requests from duplicate client retries |
| Saga State Machine | make transaction progress explicit and safe under duplicate replies |
| Outbox Row Claiming | allow multiple service instances without duplicate publishing |
| Dead Letter Queues | isolate poison messages instead of retrying forever |
| Correlation IDs | trace one user request across gateway, services, and RabbitMQ |
| Contract Versioning | evolve events without breaking other services |
| API Smoke Surface | expose enough read endpoints to verify async flows after deployment |
| CD Pipeline | build and publish deployable images after the manual deployment path is proven |
| Kubernetes Ingress | expose gateway through nginx-ingress instead of direct NodePort access |
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

### Milestone 5: API Smoke Surface and Local Deployment Checks

Status: first pass started

Goal: make the platform easy to verify after local Docker or Minikube deployment.

Work:

- Keep Dockerfiles for each service.
- Standardize local Kubernetes image tags across all services.
- Add `GET /transactions/{id}` so a created transaction can be checked after the async saga reply.
- Add a user-scoped transaction listing endpoint when it becomes useful for manual verification.
- Add `GET /campaigns` so deployment smoke tests can inspect available campaigns without direct database access.
- Keep gateway routes working for all smoke-test endpoints.
- Keep service and controller tests around the read endpoints.
- Document a minimal manual smoke checklist for Docker Compose and Minikube.

Done when:

- A user can create a transaction and then check its status through the gateway.
- Campaign data can be inspected through public API routes.
- Manual Minikube verification does not require reading database tables directly.
- Local image tags are consistent across the Kubernetes manifests.

### Milestone 6: Kubernetes Orchestration

Status: first pass started

Goal: keep the platform running in Minikube first, then prepare the same base for AWS EKS.

Work:

- Keep the existing `k8s/base` Kustomize structure.
- Keep namespace, ConfigMap, Secret references, infra manifests, app Deployments, Services, and probes.
- Keep the committed Secret as a safe template and the real local Secret ignored.
- Keep replicas at `1` until outbox publishing is safe for multiple instances.
- Maintain a repeatable Minikube smoke path.
- Add Minikube overlay when base manifests start needing local-only differences.
- Add nginx-ingress later, after the API smoke surface is useful enough to expose through a stable ingress route.
- Add EKS overlay after local Kubernetes behavior is stable.
- Consider Helm or Kustomize overlays after raw manifests work.

Done when:

- Platform runs in Minikube from a clean local setup.
- Gateway reaches backend services inside the cluster.
- Smoke endpoints pass through the gateway.
- The same base manifests can be adapted for EKS.

### Milestone 7: Docker Images and CD Pipeline

Status: planned

Goal: build and publish versioned service images after the manual deployment path is proven.

Work:

- Add GitHub Actions CD workflow on a separate branch.
- Build images only after CI passes.
- Push images to DockerHub first or AWS ECR later.
- Tag images with branch and commit SHA.
- Keep registry credentials in GitHub Actions secrets, not in Git.
- Make the image tags used by Kubernetes manifests match the tags produced by CD.
- Add a root `Jenkinsfile` only if Jenkins remains part of the learning goal.
- Optionally add image scanning.

Done when:

- CD builds all service images from a clean checkout.
- Images are traceable to Git commits.
- Registry credentials are not committed.
- The image tags used by Kubernetes manifests are produced by the pipeline.

### Milestone 8: Scale Safety and Messaging Hardening

Status: planned

Goal: make the async flow safe before running multiple service replicas.

Work:

- Harden outbox publishers in transaction and campaign services before running multiple replicas.
- Add atomic outbox row claiming so two instances cannot publish the same row.
- Recover stuck `PROCESSING` outbox rows.
- Add RabbitMQ DLQs for saga queues.

Done when:

- Outbox rows cannot be published by two service instances at the same time.
- Poison messages are moved to DLQ.
- Stuck outbox rows are recovered or made visible.
- Campaign and transaction services can be safely scaled beyond one replica.

### Milestone 9: AWS and Linux Provisioning

Status: planned

Goal: move from laptop-only execution to secure cloud infrastructure.

Work:

- Choose EC2 or EKS as the first AWS target.
- Provision Linux or EKS infrastructure.
- Configure SSH key access if EC2 is used.
- Disable password login and root login if EC2 is used.
- Restrict inbound ports.
- Keep PostgreSQL, Redis, and RabbitMQ internal.
- Store secrets in AWS SSM Parameter Store or Secrets Manager.

Done when:

- Services can run on AWS-managed or AWS-hosted infrastructure.
- Only gateway/API entry points are public.
- Secrets are managed outside Git.

### Milestone 10: Observability

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

### Milestone 11: Production Hardening

Status: later

Goal: reduce operational and security risks.

Work:

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

## Current Focus

The current focus is making the working Minikube deployment easier to verify through normal API calls:

- Keep the Maven reactor green on every relevant push and pull request.
- Add `GET /transactions/{id}` for checking transaction status after creation.
- Add enough campaign read endpoints to support smoke testing without database inspection.
- Keep gateway routes updated for the new smoke-test endpoints.
- Document the manual Minikube smoke checklist that already works locally.
- Keep migration-backed tests green.

Deferred correctness notes:

- Request idempotency stays on the roadmap, but deeper changes should wait for a reproduced bug or failing test.
- Outbox publisher hardening is scheduled near the Kubernetes milestone, where multiple replicas make duplicate publishing a real operational risk.
- CD is intentionally delayed until the manual Kubernetes path and API smoke surface are stable.

## Short-Term Backlog

- Add `GET /transactions/{id}`.
- Add campaign listing/read endpoints needed for smoke tests.
- Document Docker Compose and Minikube smoke commands.
- Add correlation IDs and event IDs.

## Long-Term Backlog

- GitHub Actions or Jenkins image publishing pipeline.
- nginx-ingress for Kubernetes gateway exposure.
- AWS EC2 or EKS provisioning.
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
