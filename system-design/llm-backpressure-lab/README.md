# LLM Backpressure Lab

A local, zero-cloud-cost architecture lab for proving how to protect a core business API from slow, flaky LLM dependencies.

This is not an AI wrapper. It is a resilience lab: the first slice creates a controllable Mock LLM; later slices add a naive synchronous baseline, an async outbox path, Kafka, bounded workers, DLQ, backpressure, k6, Prometheus, and Grafana.

## Core Claim

Slow AI dependencies must not sit inside the critical request path of a core business system.

The module proves this through a before/after lab:

- **Before:** a core API calls a slow LLM synchronously and collapses under contention.
- **After:** the core API writes accepted work to an outbox, workers process LLM tasks asynchronously, and the system applies measured backpressure when the backlog grows.

The strong claim is deliberately bounded:

> zero accepted-event loss within the defined failure envelope.

See [PROOF_CONTRACT.md](PROOF_CONTRACT.md) for exact claims, non-claims, and pass/fail criteria.

## Current Slice

Implemented now:

- `services/mock-llm/`: a Python stdlib HTTP service that simulates LLM latency, rate limits, server errors, and timeouts.
- `docker-compose.yml`: runs the Mock LLM with resource limits.
- `Makefile`: local test, run, smoke, and compose commands.

Not implemented yet:

- core API;
- outbox publisher;
- Kafka;
- AI worker;
- DLQ;
- k6 load tests;
- Prometheus/Grafana dashboards.

## Commands

Run unit tests:

```bash
make test
```

Run Mock LLM locally:

```bash
make mock-llm
```

In another terminal:

```bash
make smoke-mock-llm
```

Run with Docker Compose:

```bash
make up
make smoke-mock-llm
make down
```

## Configuration

The Mock LLM uses environment variables:

| Variable | Default | Meaning |
|---|---:|---|
| `MOCK_LLM_PORT` | `8088` | HTTP listen port. |
| `MOCK_LLM_MIN_LATENCY_MS` | `2000` | Minimum simulated latency. |
| `MOCK_LLM_MAX_LATENCY_MS` | `5000` | Maximum simulated latency. |
| `MOCK_LLM_RATE_LIMIT_PERCENT` | `10` | Percent of requests returning HTTP 429. |
| `MOCK_LLM_ERROR_PERCENT` | `5` | Percent returning HTTP 500. |
| `MOCK_LLM_TIMEOUT_PERCENT` | `5` | Percent delayed beyond normal max latency. |
| `MOCK_LLM_SEED` | unset | Optional deterministic random seed for reproducible tests. |

## Planned Architecture

```text
client/k6
  -> core-api
  -> business db + outbox
  -> outbox publisher
  -> kafka ai.tasks
  -> bounded ai-worker pool
  -> mock-llm
  -> result store or DLQ
  -> prometheus/grafana
```

## Evidence Gates

The module should not be considered mature until it can show:

- before/after k6 numbers;
- Grafana screenshots for API latency, consumer lag, worker throughput, error rate, and DLQ count;
- crash recovery without duplicate final results;
- documented production gaps;
- at least three ADRs explaining Kafka/outbox, bounded worker concurrency, and idempotency strategy.
