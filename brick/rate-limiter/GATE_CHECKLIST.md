# Gate Checklist - Rate Limiter

## Correctness

- [x] Token Bucket algorithm implemented.
- [x] HTTP 429 returned when bucket is empty.
- [x] Configurable capacity and refill rate.
- [x] Integration tests verify allow, reject, and refill behavior.
- [ ] Thread safety verified under concurrent load (same client key).
- [ ] Retry-After header included in 429 response.
- [ ] Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining) included.

## Architecture

- [x] Algorithm choice is documented and justified.
- [x] In-memory state limitation is documented.
- [x] Production extension path (distributed) is documented.
- [x] Goals and non-goals are documented.
- [ ] Client key strategy decision is documented.
- [ ] Distributed design with Redis is designed.

## Implementation

- [x] Module wired into Maven reactor.
- [x] Spring Boot application starts.
- [x] Rate limiter applied as filter/interceptor.
- [x] Integration tests exist.
- [ ] Concurrent access test for same client key.
- [ ] Monotonic clock used for refill calculation.

## Operations

- [ ] Reject rate metric exposed.
- [ ] Remaining tokens per client observable.
- [ ] Retry-After header guides client backoff.
- [ ] Runbook exists.

## Failure

- [x] Initial failure modes are documented.
- [ ] Multi-instance quota bypass is addressed.
- [ ] Race condition in tryConsume is verified.
- [ ] Clock skew handling is documented.

## Engineering Communication

- [x] README exists.
- [x] Design doc exists.
- [x] Engineering narrative exists.
- [ ] Algorithm comparison and trade-offs are documented.
- [ ] Distributed extension design documented.
