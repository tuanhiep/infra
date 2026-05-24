# Gate Checklist: LLM Backpressure Lab

## Correctness

- [x] Mock LLM returns deterministic response shapes for success and failures.
- [ ] Accepted event accounting reconciles completed, DLQ, and pending counts.
- [ ] Worker crash recovery produces no duplicate final result.

## Architecture

- [x] Proof contract defines claims and non-claims.
- [ ] Design doc covers core API, outbox, Kafka, workers, and backpressure.
- [ ] ADRs exist for event bus, idempotency, and bounded concurrency.

## Implementation

- [x] Mock LLM service exists.
- [x] Mock LLM tests exist.
- [ ] Core API exists.
- [ ] AI worker exists.
- [ ] Kafka/DLQ topology exists.

## Operations

- [ ] Prometheus metrics exist.
- [ ] Grafana dashboard export exists.
- [ ] Operations runbook exists.

## Scale And Failure Evidence

- [ ] k6 naive sync failure scenario exists.
- [ ] k6 async/backpressure scenario exists.
- [ ] Before/after metrics are captured.

## Narrative

- [ ] README includes before/after screenshots.
- [ ] Engineering narrative exists.
- [ ] Public summary is drafted after measured evidence exists.
