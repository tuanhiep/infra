# ADR-001: Use A Mock LLM For Failure Injection

Status: accepted.

## Context

The lab needs to prove how slow, flaky AI dependencies affect a core business system. Real LLM APIs would introduce cost, credentials, vendor variability, and rate-limit behavior that cannot be reliably reproduced by every reviewer.

## Decision

Use a local Mock LLM service that simulates:

- 2-5 second latency;
- HTTP 429;
- HTTP 500;
- HTTP 504;
- configurable failure distribution.

## Consequences

Positive:

- zero cloud cost;
- deterministic local reproduction;
- no secrets;
- safe for public GitHub.

Negative:

- does not prove real vendor integration;
- does not model token streaming, tool calls, or provider-specific retry headers yet.

## Follow-Up

Add real provider integration only as an optional extension after the local proof is complete.
