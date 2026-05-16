# Design Doc - Rate Limiter

## Problem

Without rate limiting, a single misbehaving or malicious client can exhaust server resources, degrade service for all other clients, or drive up infrastructure costs. Rate limiting enforces a usage contract: each client gets a quota, and requests beyond that quota are rejected.

## Goals

- Demonstrate Token Bucket algorithm with configurable capacity and refill rate.
- Enforce per-client quota with HTTP 429 on exhaustion.
- Make the algorithm mechanics explicit and testable.

## Non-Goals

- Distributed rate limiting across multiple instances.
- Per-endpoint rate limit policies.
- Persistent quota state across restarts.
- Production-grade observability (reject rate metrics, dashboards).

## Architecture

```text
HTTP request
  └─▶ RateLimiterConfiguration (servlet filter)
        └─▶ ClientRateLimiter.tryConsume(clientKey)
              └─▶ TokenBucket(capacity, refillRate)
                    ALLOWED → request proceeds
                    REJECTED → HTTP 429
  └─▶ TimeController (demo endpoint)
```

## Algorithm: Token Bucket

- **Capacity**: maximum burst size (tokens in full bucket).
- **Refill rate**: tokens added per time unit (sustained throughput).
- **tryConsume**: atomically check and decrement. Returns ALLOWED or REJECTED with remaining count.
- **Lazy refill**: tokens are accumulated since last access time, capped at capacity.

## Algorithm Comparison

| Algorithm | Burst | Smoothness | Complexity |
|---|---|---|---|
| Token Bucket | ✓ | Medium | Low |
| Leaky Bucket | ✗ | High | Medium |
| Fixed Window | ✓ boundary spike | Low | Very low |
| Sliding Window | ~ | High | High |

Token Bucket chosen for: burst tolerance, predictable sustained rate, and simple implementation.

## Trade-offs

| Decision | Alternative | Reason |
|---|---|---|
| Token Bucket | Fixed Window | Burst tolerance needed for realistic API clients |
| Per-client in-memory map | Shared Redis | Demo scope; production needs distributed state |
| Lazy refill | Scheduled refill | Simpler; no background thread |
| IP-based client key | API key | Demo simplicity; production needs auth-based key |

## Production Gaps

- In-memory state does not work with horizontal scaling; need Redis atomic operations.
- No `Retry-After` header; clients have no signal for when to retry.
- No rate limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`).
- No per-endpoint policy; all endpoints share the same bucket configuration.
- No quota persistence; restart resets all buckets to full.
- No reject rate metric; impossible to detect abuse or tune limits without data.
