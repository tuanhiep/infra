# Infra

Executable infrastructure bricks for backend resilience, traffic control, and edge behavior.

Each brick isolates one reusable mechanism and keeps its claims tied to running code,
executable tests, runtime configuration, or an explicit production gap.

## Bricks

| Brick | Mechanism | Evidence surface |
|---|---|---|
| [`cdn-edge-cache`](brick/cdn-edge-cache/) | Spring Boot origin behind an Nginx edge with cache policy, cache locking, perimeter rate limiting, and HIT/MISS signals. | Origin header integration tests plus Dockerized edge smoke procedures. |
| [`circuit-breaker`](brick/circuit-breaker/) | Resilience4j around a real HTTP client with failure classification, bounded calls, ordered fallback, and breaker telemetry. | Unit and HTTP integration tests, Actuator health, Micrometer metrics, and Prometheus export. |
| [`rate-limiter`](brick/rate-limiter/) | Per-client token-bucket admission with immediate rejection, response headers, idle-bucket eviction, and metrics. | Integration tests for independent clients, exhaustion behavior, HTTP 429, and Actuator metrics. |

## Engineering Contract

A brick should let a reviewer trace:

```text
invariant -> boundary -> implementation -> executable proof -> production gap
```

The repository intentionally keeps these components narrow. Cross-component systems,
business workflows, and broad platform claims belong in standalone projects where their
architecture, operations, and evidence can be reviewed without monorepo context.

## Stack

- Java 21
- Spring Boot 4.0.6
- Maven Wrapper 3.9.9
- Resilience4j, Micrometer, Actuator, and Prometheus where relevant
- Docker Compose and Nginx for edge runtime behavior

## Build

Run every brick test from the repository root:

```bash
./mvnw clean test
```

Run one brick:

```bash
./mvnw -pl brick/circuit-breaker test
```

Each brick README contains its runtime commands, test matrix, and explicit production gaps.
