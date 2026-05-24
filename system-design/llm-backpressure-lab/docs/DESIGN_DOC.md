# Design Doc: LLM Backpressure Lab

## Problem Statement

Enterprise systems increasingly call LLMs for analysis, summarization, enrichment, and decision support. Those dependencies are slow, rate-limited, and failure-prone. If a core API calls them synchronously, AI latency can turn into thread starvation, timeout storms, and business-path instability.

## Goals

- Build a reproducible local lab for slow/flaky LLM dependency isolation.
- Demonstrate naive synchronous failure before showing the async design.
- Keep claims bounded by a written proof contract.
- Make latency, backlog, failure, and backpressure observable.

## Non-Goals

- Real LLM API calls.
- Multi-model routing or ensemble inference.
- Multi-region Kafka durability.
- Production-ready platform claims.

## Current Architecture Slice

The current slice implements only the Mock LLM dependency and local command surface. It is intentionally small so that failure injection is controlled before the core API and Kafka paths are added.

```text
developer
  -> make mock-llm
  -> services/mock-llm
  -> configurable latency and failure responses
```

## Target Architecture

```text
client/k6 -> core-api -> business db + outbox -> kafka -> ai-worker -> mock-llm
                                                \-> metrics -> prometheus/grafana
```

## Key Design Decisions

- Use a Mock LLM instead of real APIs to make failure reproducible and zero-cost.
- Keep the first implementation slice small enough to test without Kafka.
- Use the proof contract as the acceptance boundary before adding architecture complexity.

## Open Questions

- Which language should the core API use: Spring Boot for repo consistency, or Python/FastAPI for faster local lab iteration?
- Should the outbox use Postgres/Testcontainers later, or a lightweight local DB for first proof?
- What backpressure policy should be the first public demo: 429, 202 queued, feature degradation, or a combination?
