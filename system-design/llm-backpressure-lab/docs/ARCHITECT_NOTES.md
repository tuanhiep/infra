# Architect Notes: LLM Backpressure Lab

## Intent

This module studies resilience patterns for systems that depend on slow, unreliable AI services.

The key question:

> How do we add slow, flaky AI capability without letting it collapse the core business path?

## Design Pressure

A synchronous LLM call couples API availability to external latency. Once enough requests hold threads for seconds at a time, the failure can spread from one slow dependency to the API thread pool, database pool, and client retry behavior.

The target architecture separates:

- fast acceptance of business work;
- durable accounting of accepted AI-dependent tasks;
- bounded asynchronous execution;
- explicit terminal states for completed and failed tasks;
- observable backlog growth and backpressure.

## First Slice Rationale

The first implementation slice is a configurable Mock LLM. This comes before Kafka or workers because every later failure mode depends on controlled latency and error injection.

The Mock LLM is intentionally built with the Python standard library to keep the local proof lightweight:

- no external API key;
- no cloud cost;
- no framework dependency before the system topology is proven;
- easy execution in Docker or directly on a developer machine.

## Review Notes

A later implementation should avoid hiding the failure behind a polished demo. The naive synchronous path should remain deliberately simple so that the before/after comparison is honest and measurable.

The async path should not claim success until accepted work can be reconciled against completed, DLQ, and pending states.
