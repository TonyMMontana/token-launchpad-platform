# Token Launchpad Platform

Token Launchpad Platform is a Java 21 Spring Boot microservices project built to learn real distributed-system patterns: API Gateway, JWT authentication, RabbitMQ messaging, saga coordination, transactional outbox, idempotency, Testcontainers, CI/CD, Docker, Kubernetes, and observability.

## Project Status

Status: active development

Current focus:

- Request idempotency for transaction creation
- Saga correctness
- Transactional outbox hardening
- Integration testing with Testcontainers
- Preparing the project for CI/CD and cloud deployment

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
| `transaction-service` | Transaction creation, saga start, saga reply handling |
| `campaign-service` | Campaign lifecycle, token reservation, Redis caching |
| `discovery-server` | Eureka service registry |
| `launchpad-common` | Shared event contracts |

## Infrastructure

The local environment uses:

- PostgreSQL
- Redis
- RabbitMQ
- Docker Compose
- Maven multi-module build
- Testcontainers for integration tests

## Implemented Patterns

- API Gateway
- Service Discovery
- Database per Service
- Saga Pattern
- Transactional Outbox
- Idempotent Consumer
- Cache Aside
- Shared Message Contracts
- Integration Testing with Testcontainers

## Patterns in Progress

- Request idempotency with `X-Idempotency-Key`
- Outbox publisher hardening
- Saga state machine
- RabbitMQ dead-letter queues
- Database migrations with Flyway or Liquibase
- Correlation IDs across HTTP and messaging
- Prometheus and Grafana observability

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- Spring AMQP
- RabbitMQ
- PostgreSQL
- Redis
- Eureka
- Maven
- Docker Compose
- Testcontainers

## Local Development

Start local infrastructure:

```bash
docker compose up -d
```

Build all modules:

```bash
mvn clean verify
```

Build without tests:

```bash
mvn -DskipTests compile
```

Run a focused service test suite:

```bash
mvn test -pl campaign-service -am
```

## Main Learning Topics

### Idempotency

HTTP clients may retry a request after a timeout. Without idempotency, one user action can create multiple transactions. The transaction API is being extended to support `X-Idempotency-Key`.

Target behavior:

- Same user + same key + same payload returns the original transaction.
- Same user + same key + different payload returns conflict.
- Concurrent duplicate requests create only one transaction.

### Saga

Transaction creation and token reservation happen across different services. The system uses RabbitMQ events instead of a distributed database transaction.

Current flow:

1. `transaction-service` creates a pending transaction.
2. `transaction-service` stores an outbox event.
3. Outbox publisher sends `ReserveTokensEvent`.
4. `campaign-service` reserves tokens idempotently.
5. `campaign-service` stores a reply outbox event.
6. Reply publisher sends success or failure.
7. `transaction-service` updates transaction status.

### Transactional Outbox

Outbox is used so database writes and outgoing messages are not split into unsafe operations. The next hardening step is safe row claiming for multiple service instances.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for the full build-in-public roadmap.

Near-term work:

- Finish robust request idempotency
- Add transaction saga reply idempotency
- Add Flyway or Liquibase migrations
- Harden outbox row claiming and stuck-row recovery
- Add RabbitMQ DLQs
- Add GitHub Actions CI
- Add Dockerfiles for every service

## Why This Project Exists

This project is my practical path to learning microservice architecture. Instead of only reading about patterns, I am implementing them in a system where the tradeoffs become visible:

- retries can create duplicates
- messages can be delivered more than once
- publishing can fail after a database commit
- schemas must evolve safely
- distributed flows need logs, metrics, and correlation IDs

## Build in Public Notes

I am using this repository to document not only final code, but also the reasoning behind architectural choices.

Example topics I am exploring:

- Why idempotency needs more than a unique key
- Why outbox needs publisher confirms and retry state
- Why duplicate RabbitMQ messages are normal
- Why schema migrations matter in microservices
- How to test distributed flows with Testcontainers

## Current Limitations

- Deployment manifests are not complete yet.
- Database migrations are planned but not fully implemented.
- Observability dashboards are planned.
- Outbox row claiming still needs production-grade hardening.
- Request idempotency is being actively improved.

## License

This project is currently for learning and portfolio purposes.
