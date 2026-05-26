# Gate Checklist - CDN Edge Cache

## Correctness

- [x] Edge caches GET/HEAD responses according to Origin's Cache-Control headers.
- [x] POST mutations are not cached (no-store enforced).
- [x] Static assets with content hash have long TTL.
- [x] Semi-static API has short TTL with stale-while-revalidate.
- [x] HIT/MISS is observable via X-Cache-Status header.
- [ ] Authorization header bypasses cache (data leak prevention).
- [ ] proxy_cache_lock prevents thundering herd.

## Architecture

- [x] Cache policy is owned by Origin, enforced by Edge.
- [x] Edge and Origin have clear responsibility boundaries.
- [x] Goals and non-goals are documented.
- [x] Trade-offs are documented.
- [ ] Multi-instance cache consistency strategy is documented.

## Implementation

- [x] Module runs via Docker Compose.
- [x] Nginx Edge configuration exists.
- [x] Spring Boot Origin exists with correct Cache-Control headers.
- [x] Integration tests verify HIT/MISS behavior.
- [ ] proxy_cache_lock is configured.
- [ ] Authorization bypass is configured and tested.

## Operations

- [x] X-Cache-Status header exposes cache behavior.
- [ ] Nginx access log captures HIT/MISS rate.
- [ ] Prometheus metrics from Nginx exist.
- [ ] Runbook exists.

## Failure

- [x] Initial failure modes are documented.
- [ ] Thundering herd is mitigated.
- [ ] Stale fallback on Origin failure is configured.
- [ ] Cache poisoning vectors are addressed.

## Engineering Communication

- [x] README exists.
- [x] Design doc exists.
- [x] Engineering narrative exists.
- [ ] Key design trade-offs are summarized for design review.
- [ ] Production gaps are explicitly documented with next steps.
