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
- `discovery-server`: Eureka service registry.
- `launchpad-common`: shared event contracts used by messaging flows.

### Infrastructure

- PostgreSQL for service persistence.
- Redis for campaign caching.
- RabbitMQ for asynchronous saga messaging.
- Docker Compose for local infrastructure.
- Maven root reactor for multi-module builds.

## What Is Already Implemented

- Basic service split across gateway, auth, campaign, transaction, discovery, and shared contracts.
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
- Initial roadmap for CI/CD, AWS, Kubernetes, and observability.

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
| Service Discovery | Eureka through `discovery-server` |
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
| Flyway or Liquibase | replace Hibernate schema auto-update with repeatable migrations |
| Request Idempotency | protect public POST requests from duplicate client retries |
| Saga State Machine | make transaction progress explicit and safe under duplicate replies |
| Outbox Row Claiming | allow multiple service instances without duplicate publishing |
| Dead Letter Queues | isolate poison messages instead of retrying forever |
| Correlation IDs | trace one user request across gateway, services, and RabbitMQ |
| Contract Versioning | evolve events without breaking other services |
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

Status: current focus

Goal: make the distributed transaction flow safe under duplicate messages, retries, partial failures, and concurrent requests.

Work:

- Finish request idempotency in `transaction-service`.
- Store idempotency keys scoped by user.
- Reject same idempotency key with different request payload.
- Add idempotency to saga reply handling in `transaction-service`.
- Add explicit transaction saga states.
- Harden outbox publishers in transaction and campaign services.
- Recover stuck `PROCESSING` outbox rows.
- Add DLQs for saga queues.

Done when:

- Duplicate HTTP transaction requests create one transaction.
- Duplicate RabbitMQ messages do not change state twice.
- Outbox rows cannot be published by two service instances at the same time.
- Poison messages are moved to DLQ.

### Milestone 3: Database Migrations

Status: planned

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

Status: planned

Goal: validate the platform automatically on every push.

Work:

- Add `.github/workflows/ci.yml`.
- Use Java 21.
- Cache Maven dependencies.
- Run `mvn clean verify`.
- Ensure Docker is available for Testcontainers.
- Upload test reports on failure.

Done when:

- Every push runs the test suite.
- Integration tests run in the cloud.
- Failures expose useful logs.

### Milestone 5: Docker Images and Jenkins Pipeline

Status: planned

Goal: build and publish versioned service images.

Work:

- Add Dockerfiles for each service.
- Add a root `Jenkinsfile`.
- Build only after tests pass.
- Push images to DockerHub first or AWS ECR later.
- Tag images with branch and commit SHA.
- Optionally add image scanning.

Done when:

- Jenkins builds all service images from a clean checkout.
- Images are traceable to Git commits.
- Registry credentials are not committed.

### Milestone 6: AWS and Linux Provisioning

Status: planned

Goal: move from laptop-only execution to secure Linux infrastructure.

Work:

- Provision EC2 instances.
- Configure SSH key access.
- Disable password login and root login.
- Restrict inbound ports.
- Keep PostgreSQL, Redis, RabbitMQ, and Eureka internal.
- Store secrets in AWS SSM Parameter Store or Secrets Manager.

Done when:

- Services can run on Linux.
- Only gateway/API entry points are public.
- Secrets are managed outside Git.

### Milestone 7: Kubernetes Orchestration

Status: planned

Goal: deploy the platform to Minikube first, then AWS EKS.

Work:

- Create `k8s/` manifests.
- Add Deployments, Services, ConfigMaps, and Secret references.
- Add readiness and liveness probes.
- Decide whether Eureka remains useful inside Kubernetes.
- Add Minikube overlay.
- Add EKS overlay.
- Consider Helm or Kustomize after raw manifests work.

Done when:

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

The current focus is request idempotency and saga correctness:

- Add `X-Idempotency-Key` support to transaction creation.
- Scope idempotency keys by user.
- Detect same key with different payload and return conflict.
- Keep transaction creation and outbox event creation in the same database transaction.
- Add tests for duplicate request retries.
- Add idempotency to transaction saga reply listeners.

## Short-Term Backlog

- Finish robust request idempotency in `transaction-service`.
- Add integration tests for request idempotency.
- Add saga reply idempotency in `transaction-service`.
- Add explicit saga state transitions.
- Add Flyway or Liquibase migrations.
- Harden outbox row claiming.
- Add stuck outbox recovery.
- Add RabbitMQ DLQs.
- Add correlation IDs and event IDs.
- Add GitHub Actions CI.
- Add service Dockerfiles.

## Long-Term Backlog

- Jenkins image publishing pipeline.
- AWS EC2 provisioning.
- Kubernetes manifests for Minikube and EKS.
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
