# Engineering Narrative - Rate Limiter

## The Problem Worth Solving

Rate limiting is a system boundary contract: each client gets a quota, and the system enforces it. The mechanics are simple to state and subtle to get right — algorithm choice affects burst behavior, implementation choice affects thread safety and scale, and key strategy affects whether the limit applies to the right unit of identity.

## What Was Built

A Spring Boot service with a custom Token Bucket rate limiter per client key. Capacity and refill rate are configurable. Requests within quota return HTTP 200; requests beyond quota return HTTP 429. Integration tests verify the allow/reject/refill cycle.

## The Judgment Behind It

**Token Bucket over Fixed Window**: Fixed Window allows up to 2× the limit at window boundaries — a legitimate burst from a well-behaved client can hit 429 at exactly the wrong moment. Token Bucket smooths this: burst is allowed up to capacity, then sustained at refill rate. For real API clients that batch requests, this matters.

**In-memory state as an explicit scope**: The module does not pretend in-memory is sufficient for production. It is the foundation: understand the algorithm before adding the distributed complexity of Redis atomic operations, Lua scripts, and network failure modes.

**Production gaps named, not hidden**: No `Retry-After` header means clients that get 429 have no backoff signal — they retry immediately, generating more load. IP-based key behind shared NAT affects all users behind it. These are not edge cases; they are the first production failures anyone deploying this would encounter.

## What This Demonstrates

- Token Bucket mechanics: capacity, refill rate, burst tolerance.
- Thread safety as a requirement, not an afterthought.
- Production gap awareness: distributed state, Retry-After, observability.
- The algorithm comparison judgment: when Token Bucket vs Fixed Window vs Sliding Window.

## Production Follow-Up

Redis atomic counter (INCR + EXPIRE + Lua script for atomicity), Retry-After header in 429 responses, rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset), per-endpoint policy, and reject rate metrics.

The distributed extension is straightforward once the single-instance algorithm is correct.
