# Failure Modes: LLM Backpressure Lab

## F1 - Naive Sync Thread Starvation

Status: planned.

The core API calls the Mock LLM synchronously. Under load, request threads are held for 2-5 seconds or longer, causing latency growth and timeout storms.

Proof target:

- k6 latency distribution;
- API thread/connection pressure;
- before-demo screenshot.

## F2 - Mock LLM Flakiness

Status: first slice implemented.

The Mock LLM can return:

- HTTP 429;
- HTTP 500;
- HTTP 504;
- delayed successful responses.

Current evidence:

- `services/mock-llm/mock_llm.py`;
- `services/mock-llm/tests/test_mock_llm.py`.

## F3 - Worker Crash Before Offset Commit

Status: planned.

Worker dies mid-task. Kafka should redeliver the task, and idempotency should prevent duplicate terminal results.

## F4 - Backlog Growth Under Spike Load

Status: planned.

k6 creates a traffic spike. Kafka lag grows, worker throughput remains bounded, and the API triggers a backpressure policy instead of accepting unlimited work.

## F5 - Silent Event Loss

Status: planned.

Accepted events must reconcile against completed, DLQ, and pending counts. Any unexplained drift is a failed proof.
