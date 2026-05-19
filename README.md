# Infra

Executable architecture case studies for backend infrastructure, distributed systems mechanics, and engineering judgment under failure.

This repository is built around a simple standard: every architectural claim should be backed by running code, tests, configuration, or an explicit production gap. It is not a pattern catalog. It is a small set of modules designed to make invariants, trade-offs, and failure behavior inspectable.

## What This Repo Optimizes For

- Correctness boundaries that are named in the design and encoded in tests.
- Failure modes written before they become hidden assumptions.
- Runnable modules, not slide-only system design.
- Operational signals such as health, metrics, cache status, and degradation state where the module needs them.
- Production gaps stated plainly so the demo does not pretend to be a finished platform.

## Current Tracks

```text
infra/
  brick/
    cdn-edge-cache/
    circuit-breaker/
    rate-limiter/
  system-design/
    idempotent-payment-ledger/
  principal-engineering/
    README.md
```

### `brick/`

Small, reusable infrastructure primitives. A brick should be independently understandable, testable, and useful as a building block for larger system design modules.

| Module | Focus | Current maturity |
|---|---|---|
| `brick/cdn-edge-cache` | Spring Boot Origin plus Nginx Edge cache, cache policy, WAF sample, rate limiting, HIT/MISS observability | Strong runnable brick; Dockerized Edge exists; remaining work is deeper runtime evidence, cache security hardening, and production observability. |
| `brick/circuit-breaker` | Resilience4j CircuitBreaker around a real HTTP client, typed failure taxonomy, ordered fallback, Actuator/Micrometer/Prometheus signals | Strongest brick; good correctness and observability surface; remaining work is bulkhead isolation, retry policy decision, runbook, and dashboard. |
| `brick/rate-limiter` | Per-client token bucket, immediate rejection, rate-limit headers, Actuator metrics | Solid foundational brick; remaining work is concurrency/load evidence, `Retry-After`, monotonic-clock handling, and distributed quota design. |

### `system-design/`

Runnable case studies that combine multiple infrastructure concerns into a coherent system with requirements, failure analysis, and a path toward production hardening.

| Module | Focus | Current maturity |
|---|---|---|
| `system-design/idempotent-payment-ledger` | Retry-safe payment intake, idempotency keys, payload hashing, duplicate replay, conflict detection, balanced debit/credit ledger entries | Strong first slice; correctness invariants are clear and tested; next serious step is durable transaction boundary with Postgres-style persistence, ledger posting rules, outbox, reconciliation, and domain metrics. |

### `principal-engineering/`

Architecture review and operating-judgment material. This track is intentionally present but not yet mature: it should grow only when there are real modules, trade-offs, incidents, rollout plans, or red-team reviews worth capturing.

## Evidence Gates

Each serious module should carry the same public evidence surface:

- `README.md`: problem, invariants, runtime flow, commands, test matrix, production gaps.
- `docs/DESIGN_DOC.md`: goals, non-goals, architecture, consistency model, alternatives, trade-offs.
- `docs/FAILURE_MODES.md`: concrete failure scenarios, impact, detection, mitigation, and current evidence.
- `docs/ARCHITECT_NOTES.md`: design pressure points and review notes.
- `docs/ENGINEERING_NARRATIVE.md`: how to explain the module without hiding the hard parts.
- `GATE_CHECKLIST.md`: remaining work across correctness, architecture, implementation, operations, failure, scale, security, and narrative.

The gate is not "does it compile?" The gate is whether a serious reviewer can trace the path from invariant -> code -> test -> operational behavior -> known limitation.

## Stack

- Java 21
- Spring Boot 4.0.6
- Maven Wrapper 3.9.9
- Maven multi-module project with dependency and plugin governance in the root POM
- Resilience4j, Micrometer, Actuator, Prometheus export where relevant
- Nginx and Docker Compose for the CDN Edge cache brick

## Build

Run the full reactor:

```bash
./mvnw clean test
```

Run one module:

```bash
./mvnw -pl brick/circuit-breaker test
./mvnw -pl brick/rate-limiter test
./mvnw -pl brick/cdn-edge-cache test
./mvnw -pl system-design/idempotent-payment-ledger test
```

Run a Spring Boot module locally:

```bash
./mvnw -pl brick/circuit-breaker spring-boot:run
```

For the CDN Edge cache, build the Origin and run the Edge pair:

```bash
./mvnw -pl brick/cdn-edge-cache package
cd brick/cdn-edge-cache
docker compose up --build
```

## Engineering Bar

This repo is useful only if the modules stay honest. A module should not be treated as mature until it can answer:

- What invariant does this protect?
- What failure mode does it intentionally handle?
- What failure mode does it explicitly not handle yet?
- What is the source of truth?
- What is the transaction or consistency boundary?
- What signal would tell an operator this pattern is failing in production?
- What would change when the system moves from one instance to many?

## Current Read

The repository is past toy-demo level in several modules because the code, tests, and docs already describe real boundaries: retry safety, fallback classification, edge/origin split, token-bucket admission, and balanced ledger mutation.

It is not yet a complete production-grade platform. The next level is less about adding modules and more about hardening the most valuable ones:

1. Make `idempotent-payment-ledger` durable with transactional persistence, posting rules, outbox, reconciliation, and metrics.
2. Turn `rate-limiter` into a real quota-control-plane slice with distributed counters and policy ownership.
3. Add bulkheads, alert rules, dashboards, and runbooks to `circuit-breaker`.
4. Close CDN cache security/observability gaps and prove runtime Edge behavior beyond Origin header tests.
5. Grow `principal-engineering/` from real review packets, incident drills, rollout plans, and design decision records.

The intended direction is narrow and deep: fewer modules, stronger invariants, better failure evidence.
