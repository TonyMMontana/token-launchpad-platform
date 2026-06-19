# Token Launchpad Platform

Java 21 Spring Boot microservices project for learning production-style backend engineering: authentication, API Gateway routing, RabbitMQ-based saga flow, transactional outbox, idempotency, PostgreSQL, Redis, Kubernetes, CI, and E2E validation.

The goal is not to collect technologies. The goal is to make a small distributed system that can be built, tested, deployed, observed, and explained.

## Current Status

Active development.

Current focus:

- Keep the Maven and Kubernetes E2E CI pipeline green.
- Make local Kubernetes deployment repeatable from a clean checkout.
- Add Helm, Ingress, monitoring, logging, and deployment discipline before adding more business features.
- Keep service replicas at `1` until outbox publishing is safe for multiple instances.

## Architecture

```text
client
  |
  v
gateway-service
  |
  +--> sso-service
  |
  +--> transaction-service
           |
           | RabbitMQ: ReserveTokensEvent
           v
        campaign-service
           |
           | RabbitMQ: TokensReservedSuccessEvent / TokensReservedFailedEvent
           v
        transaction-service
```

## Services

| Service | Responsibility |
| --- | --- |
| `gateway-service` | Public API entry point, JWT validation, request routing |
| `sso-service` | User registration, login, JWT creation |
| `transaction-service` | Transaction creation, request idempotency, saga start, saga reply handling |
| `campaign-service` | Campaign lifecycle, token reservation, Redis caching |
| `launchpad-common` | Shared message contracts |

## What Is Already There

- 4 Spring Boot runtime services plus shared contracts.
- PostgreSQL, Redis, and RabbitMQ local infrastructure.
- Dockerfiles for services and Docker Compose for local infrastructure.
- First-pass Kubernetes manifests under `k8s/base`.
- Gateway routing and JWT-based authentication.
- Transaction creation and campaign token reservation flow.
- RabbitMQ saga messages between transaction and campaign services.
- Transactional outbox in transaction and campaign services.
- Request idempotency for transaction creation.
- Campaign-side reservation idempotency.
- Flyway migrations with schema validation.
- Testcontainers integration tests.
- GitHub Actions Maven CI.
- GitHub Actions kind deployment with gateway-level E2E saga tests.
- API smoke endpoints for checking transaction and campaign state after deployment.

## Tech Stack

- Java 21, Spring Boot 3.4, Maven
- Spring Cloud Gateway, Spring Security, Spring Data JPA, Spring AMQP
- PostgreSQL, Redis, RabbitMQ
- Docker, Docker Compose
- Kubernetes, Kustomize
- Testcontainers, GitHub Actions

## Local Development

Start local infrastructure:

```bash
docker compose up -d
```

Run the full build:

```bash
mvn clean verify
```

Run one service test suite:

```bash
mvn test -pl campaign-service -am
```

Build without tests:

```bash
mvn -DskipTests compile
```

## Roadmap Summary

The next work should improve employable backend depth, not add random tools.

| Priority | Area | Why |
| --- | --- | --- |
| 1 | Helm + Ingress | Turns raw manifests into a realistic deployment package and exposes the gateway properly |
| 2 | Monitoring + logging | Shows the system can be operated, not only started |
| 3 | CI/CD image pipeline | Makes builds traceable and deployable from versioned images |
| 4 | Messaging hardening | Allows safe scaling beyond one replica |
| 5 | Security hardening | Makes auth, secrets, and gateway behavior more production-like |
| 6 | Payment webhook integration | Adds a realistic business integration with retries and idempotency |

See [ROADMAP.md](ROADMAP.md) for the short implementation plan.

## Intentionally Delayed

- Cloud deployment: useful later, but local Kubernetes should be clean first.
- Service mesh: unnecessary until Kubernetes operations are mature.
- More microservices: the current services need hardening first.
- Event sourcing and CQRS: educational, but too heavy for the current stage.

## Why This Project Exists

This repository is a portfolio project for practical backend engineering. The important learning topics are:

- client retries and duplicate requests
- duplicate RabbitMQ messages
- database/message consistency through outbox
- repeatable schema migrations
- Kubernetes deployment
- CI-based E2E validation
- observability for async flows

## License

This project is for learning and portfolio purposes.
