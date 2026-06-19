# Token Launchpad Platform Roadmap

This roadmap is intentionally short. The project already has enough moving parts. The next goal is to make the system look like something a Java backend developer could deploy, observe, debug, and explain in an interview.

## Current Baseline

### What Already Exists

- 4 runtime services: `gateway-service`, `sso-service`, `transaction-service`, `campaign-service`.
- Shared event module: `launchpad-common`.
- PostgreSQL, Redis, RabbitMQ.
- Docker Compose for local infrastructure.
- Dockerfiles for services.
- Kubernetes base manifests under `k8s/base`.
- Gateway routing and JWT validation.
- Registration and login.
- Transaction creation.
- Campaign token reservation.
- RabbitMQ saga flow between transaction and campaign services.
- Transactional outbox on both sides of the saga.
- Request idempotency for transaction creation.
- Campaign reservation idempotency.
- Flyway migrations.
- Testcontainers integration tests.
- GitHub Actions Maven CI.
- GitHub Actions kind deployment with gateway-level E2E tests.

### Current Weak Points

- Kubernetes exists, but it is still raw and local-first.
- No Helm chart yet.
- No Ingress yet.
- No production-style monitoring dashboards.
- No centralized structured logging.
- Docker images are not published through a versioned deployment pipeline.
- Outbox publishing is not hardened for multiple replicas.
- RabbitMQ DLQs are not in place.
- Secrets are basic local Kubernetes templates, not production-grade secret management.

## Step 1: Helm + Kubernetes Ingress

### 1. What Is Already There

- `k8s/base` contains namespace, config, secrets, infrastructure manifests, app Deployments, and Services.
- CI can deploy the platform into a disposable kind cluster.
- Gateway-level E2E tests already prove the main saga through public routes.

### 2. Highest Impact Work

- Add a Helm chart for the whole platform or one parent chart with service templates.
- Move image names, tags, ports, environment variables, probes, resources, and replica counts into `values.yaml`.
- Add nginx Ingress for `gateway-service`.
- Keep PostgreSQL, Redis, RabbitMQ internal to the cluster.
- Document one repeatable local path: install chart, run smoke tests, uninstall chart.
- Keep replicas at `1` for transaction and campaign services until outbox row claiming is fixed.

Impact: this is the highest-value next step. Helm + Ingress is a realistic Kubernetes deployment story and is easy to discuss in interviews.

### 3. Good To Have

- Separate values files for `local`, `ci`, and future `cloud`.
- Use Helm dependency charts for PostgreSQL, Redis, and RabbitMQ later.
- Add chart linting in CI.

## Step 2: Monitoring And Logging

### 1. What Is Already There

- The system has async saga behavior where observability matters.
- Services already have clear domain events and smoke endpoints.
- RabbitMQ, Redis, PostgreSQL, and JVM services are good monitoring targets.

### 2. Highest Impact Work

- Add Spring Boot Actuator to all runtime services.
- Expose Prometheus metrics.
- Add Prometheus and Grafana to local Kubernetes.
- Build dashboards for:
  - HTTP latency and error rate
  - JVM memory and threads
  - RabbitMQ queue depth
  - saga success/failure count
  - outbox backlog
- Add structured JSON logs.
- Add correlation IDs across gateway, HTTP calls, and RabbitMQ messages.

Impact: this shows you understand operating services, not only writing code. For backend jobs this is stronger than adding another business feature.

### 3. Good To Have

- Add Loki or OpenSearch for centralized logs.
- Add OpenTelemetry tracing.
- Add alerts for high error rate, growing outbox backlog, and DLQ messages.

## Step 3: Versioned Image Build And Deployment Pipeline

### 1. What Is Already There

- GitHub Actions already runs Maven CI.
- CI already deploys to kind and runs gateway E2E tests.
- Service Dockerfiles already exist.
- Kubernetes manifests already use service images.

### 2. Highest Impact Work

- Add a deployment workflow that builds all service images after CI passes.
- Tag images with commit SHA.
- Push images to DockerHub or GitHub Container Registry.
- Make Helm/Kubernetes deploy the same tags produced by CI.
- Store registry credentials only in GitHub Actions secrets.
- Add a manual smoke checklist for deploying a specific version.

Impact: this creates a clean story from commit to tested image to Kubernetes deployment.

### 3. Good To Have

- Add Trivy image scanning.
- Add SBOM generation.
- Add rollback documentation.
- Add separate workflows for pull request validation and release publishing.

## Step 4: Messaging And Outbox Hardening

### 1. What Is Already There

- Transactional outbox exists in transaction and campaign services.
- RabbitMQ saga flow exists.
- Campaign reservation consumer is idempotent.
- Request idempotency exists for transaction creation.

### 2. Highest Impact Work

- Add atomic outbox row claiming so multiple service instances cannot publish the same event.
- Add stuck outbox row recovery.
- Add RabbitMQ dead-letter queues.
- Add retry limits and poison-message handling.
- Add tests for duplicate messages, failed publishes, and stuck outbox rows.

Impact: this is what makes scaling from one replica to multiple replicas credible.

### 3. Good To Have

- Add event version fields.
- Document message contracts.
- Add compensation flow for releasing reserved tokens after failed/expired transactions.

## Step 5: Security Hardening

### 1. What Is Already There

- `sso-service` issues JWTs.
- `gateway-service` validates JWTs.
- Services receive user context through gateway headers.
- Kubernetes has basic Secret manifests.

### 2. Highest Impact Work

- Make gateway the only public entry point.
- Add rate limiting on sensitive routes.
- Strengthen secret handling for local Kubernetes and CI.
- Add clear separation between public routes and internal routes.
- Validate internal headers so they cannot be spoofed from outside the gateway path.

Impact: this improves the trust boundary story, which matters in enterprise backend interviews.

### 3. Good To Have

- Add Keycloak or another OIDC provider later.
- Add service-to-service authentication.
- Add refresh tokens only if the auth flow becomes more realistic.

## Step 6: Payment Webhook Integration

### 1. What Is Already There

- The domain already has transactions and campaign token reservation.
- Request idempotency and saga patterns are already relevant.
- RabbitMQ can handle async payment-result processing.

### 2. Highest Impact Work

- Add a normal payment-provider-style integration before crypto payments.
- Implement payment intent creation, payment status, and webhook processing.
- Verify webhook signatures.
- Make webhook handling idempotent.
- Store external payment IDs and audit events.
- Add tests for duplicate webhooks, failed payments, and late payment success/failure.

Impact: payment webhooks are a realistic business integration and give strong examples of idempotency, security, and async processing.

### 3. Good To Have

- Add a fake local payment provider for E2E tests.
- Add Stripe sandbox integration if using an external provider becomes acceptable.
- Add crypto payment integration only after the normal payment flow is solid.

## What Not To Prioritize Now

- AWS/EKS: useful later, but do not start there before Helm, Ingress, monitoring, and versioned images are clean locally.
- Jenkins: GitHub Actions is enough unless a target job specifically asks for Jenkins.
- Service mesh: too early.
- Kafka: RabbitMQ already gives enough messaging depth for this project.
- More services: harden the current services first.
- Event sourcing: too much complexity for the current value.
- Crypto payments: interesting for the project theme, but less generally employable than normal payment webhooks.

## Recommended Order

1. Helm chart and Ingress.
2. Prometheus, Grafana, Actuator metrics, structured logs, correlation IDs.
3. Versioned Docker image publishing and deployment workflow.
4. Outbox row claiming, stuck-row recovery, RabbitMQ DLQs.
5. Gateway/security hardening.
6. Payment webhook integration.
7. Cloud deployment after the local production story is strong.

## Portfolio Target Sentence

The project should eventually be explainable like this:

> I built a Java 21 Spring Boot microservice platform with API Gateway authentication, PostgreSQL, Redis, RabbitMQ saga messaging, transactional outbox, request idempotency, Flyway migrations, Testcontainers integration tests, Kubernetes deployment with Helm and Ingress, CI-based E2E tests, Prometheus/Grafana monitoring, structured logs, and versioned Docker image delivery.
