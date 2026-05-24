# Proof Contract: LLM Backpressure Lab

This module is a reproducible local lab for proving how a core business system can isolate slow or unreliable LLM dependencies without pretending to be a production platform.

The lab starts with one concrete dependency: a configurable Mock LLM service. Later slices add a core API, outbox publisher, Kafka, bounded workers, DLQ handling, k6 load tests, Prometheus, and Grafana.

## Claims

### C1 - Core API Isolation

The async design should keep the core API response path fast while slow LLM work runs outside the request thread.

Target after async implementation:

- P95 core API latency under 200 ms during normal accepted load.
- LLM task completion latency is measured separately from API response latency.

### C2 - Accepted-Event Accounting

Every event accepted by the core API and written to the outbox must end in exactly one terminal state within the defined failure envelope:

- completed;
- failed and routed to DLQ;
- explicitly pending during an active backlog.

The claim is **not** absolute losslessness under every possible infrastructure failure. The claim is **zero accepted-event loss within the defined failure envelope**.

### C3 - Idempotent Worker Recovery

If an AI worker dies after receiving a Kafka task but before committing its offset, another worker should be able to process the task without creating duplicate final results.

### C4 - Measured Backpressure

Consumer lag, queue depth, worker throughput, error rate, and DLQ count must be visible. When lag crosses the configured threshold, the system must shed, degrade, or reject additional AI-dependent work instead of accepting unbounded load.

### C5 - Before/After Reproduction

The lab must produce two repeatable demos:

- **Before:** naive synchronous LLM call causes thread starvation, timeout growth, and poor API latency under load.
- **After:** async outbox plus Kafka plus bounded workers prevents slow LLM calls from collapsing the core API path.

## Non-Claims

| Non-claim | Reason |
|---|---|
| Production-ready platform | This is a focused architecture lab. |
| Absolute losslessness under every infrastructure failure | Kafka broker crash, disk loss, network partition, and multi-region disaster recovery are outside the initial envelope. |
| 10k requests per second | Throughput depends on the machine running the lab and must be measured, not promised. |
| Real LLM integration | The Mock LLM intentionally simulates latency and failures without cloud cost. |
| Kafka HA | Initial implementation uses local Docker topology for reproducibility. |
| Flat end-to-end P99 latency | Slow LLM tasks still take seconds. The goal is to protect core API latency and make backlog behavior observable. |

## Failure Envelope

Claims apply only when:

- Docker runtime is healthy.
- Local Kafka is running for Kafka-dependent slices.
- The core database/outbox is available.
- Mock LLM latency and error rates match the configured scenario.
- Kafka retention is sufficient for the test duration.
- Worker retry and idempotency configuration match the demo scenario.

Anything outside this envelope must be documented as a production gap, not hidden behind stronger claims.

## Failure Modes To Prove

| ID | Failure mode | Expected proof |
|---|---|---|
| F1 | Naive synchronous LLM calls block request threads | k6 and metrics show API latency/timeout growth. |
| F2 | Worker dies mid-task | Task is reprocessed and final result is not duplicated. |
| F3 | Mock LLM returns 429, 5xx, or timeout | Worker retries with limits and routes terminal failure to DLQ. |
| F4 | Traffic spike creates backlog | Kafka lag grows in a measured way while core API remains protected. |
| F5 | Backpressure threshold is crossed | API rejects, sheds, or degrades AI-dependent work according to policy. |

## Pass/Fail Criteria

| Metric | Pass | Fail |
|---|---|---|
| Mock LLM configurable latency | p50/p95 follow configured bounds | fixed or unmeasured latency |
| Mock LLM configurable failures | observed status distribution matches configured scenario closely enough for lab use | failures cannot be reproduced |
| Accepted event accounting | accepted = completed + DLQ + pending | silent loss or unexplained drift |
| Duplicate final result count | 0 after worker crash scenario | duplicate terminal result |
| Backpressure behavior | threshold crossing produces a visible policy response | unbounded acceptance under overload |
| Before/after demo | both modes are runnable with one command each | demo requires manual hidden steps |

## Reproduction Commands

Current slice:

```bash
make test
make mock-llm
make smoke-mock-llm
```

Target final slice:

```bash
make up
make demo-naive
make demo-async
make inject-worker-crash
make stress-test
make down
```

## Artifact Checklist

- [x] `PROOF_CONTRACT.md` defines claims, non-claims, failure modes, and pass/fail criteria.
- [x] `services/mock-llm/` simulates latency and failures without external APIs.
- [ ] `services/core-api/` implements naive sync and async outbox paths.
- [ ] `services/ai-worker/` implements bounded Kafka consumers, retries, DLQ, and idempotency.
- [ ] `docker-compose.yml` wires the runnable local stack.
- [ ] `scripts/k6/` contains before/after load tests.
- [ ] `grafana/` exports dashboards for README screenshots.
- [ ] `docs/adr/` contains at least three ADRs.
- [ ] `README.md` shows the before/after narrative and exact reproduction commands.
