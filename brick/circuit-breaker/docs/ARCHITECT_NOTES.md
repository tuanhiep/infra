# Architect Notes - Circuit Breaker

## Ownership

This module is owned by the Principal Engineer. AI agents assist with implementation and review.

## Design Intent

This module is not a resilience library demo. It is a working model of the hardest part of circuit breaker design: **failure taxonomy**.

Most circuit breaker tutorials show the state machine. This module focuses on the boundary question: which exceptions should the breaker see, and which should it never see?

The answer shapes the entire implementation: where validation runs, how exceptions are wrapped, and what fallback applies to what.

## Key Architectural Decisions

**Failure taxonomy separation**: `RemotePaymentGatewayException` wraps all downstream infrastructure failures. `BusinessRuleException` represents domain rejection. These two must never be conflated. If they are, fallback silences business errors, making the system look healthy when it is not.

**Ordered fallback with explicit degradation signal**: `degraded=true` in the response is not optional. Downstream consumers must know they received a fallback value. Hidden degradation is worse than explicit failure.

**Per-currency TTL in cache**: Different currencies may have different volatility. A single TTL assumes all currencies are equivalent in freshness tolerance. This is a simplification acceptable for this demo; production would need configurable TTLs.

**Conservative default as last resort**: The formula `max(amount * 3.5%, 1.00)` is a business judgment, not a technical default. It must be approved by finance and legal before production use. The code makes this explicit in comments.

## AI-Assisted Work

Resilience4j configuration was reviewed with AI assistance. Failure taxonomy boundaries and exception wrapping strategy were validated manually.

Known AI pattern: AI tends to suggest catch-all fallback as a "safe" default. This is incorrect in payment systems. Every fallback boundary was explicitly validated against the taxonomy.

## Open Questions

- Should `FallbackQuoteCache` be extracted to a shared cache (Redis) for multi-instance consistency?
- At what traffic level does semaphore-based bulkhead become insufficient and thread-pool isolation is needed?
- Should the conservative default be configurable per merchant or per currency pair?
