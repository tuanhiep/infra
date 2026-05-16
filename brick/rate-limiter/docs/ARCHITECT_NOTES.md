# Architect Notes - Rate Limiter

## Ownership

This module is owned by the Principal Engineer. AI agents assist with implementation and review.

## Design Intent

This module is not a production rate limiter. It is a working model of the Token Bucket algorithm to make the mechanics explicit: capacity, refill rate, burst tolerance, and the fundamental limitation of in-memory state.

The goal is to understand what rate limiting actually does before adding distributed complexity.

## Key Architectural Decisions

**Token Bucket over Fixed Window**: Fixed Window has a boundary spike problem — 2× the limit is allowed at window boundaries. Token Bucket distributes burst more evenly while still allowing short bursts up to capacity. For API clients that may burst legitimately, Token Bucket is the more realistic choice.

**Lazy refill over scheduled refill**: Tokens are accumulated based on elapsed time since last access, not via a background scheduler. This avoids a background thread and is correct for single-instance usage. The downside: if no requests arrive, tokens accumulate silently until cap. This is the expected behavior.

**In-memory state explicitly scoped as demo**: The decision to use in-memory state is not a gap missed — it is an explicit scope boundary. The production extension path (Redis atomic INCR + TTL) is documented as the next step.

## AI-Assisted Work

Token Bucket implementation was reviewed with AI assistance for thread safety. The lazy refill calculation (`elapsed * refillRate / duration`) was validated for boundary correctness.

Known AI pattern: AI tends to skip `Retry-After` header in 429 responses. This is a practical gap that significantly impacts client behavior in production. It was added as a documented gap.

## Open Questions

- Is the current `tryConsume` synchronized correctly under high concurrency? Verify under load test.
- Should refill be clock-based (wall) or monotonic? Current implementation: document which is used.
- At what request rate does in-memory bucket lookup become a bottleneck vs Redis network hop?
