# Architect Notes - CDN Edge Cache

## Ownership

Architecture decisions and public claims are owned by the repository maintainer. Implementation and review tools do not replace engineering judgment or evidence.

## Design Intent

This module is not a CDN replacement. It is a working model of the core CDN mechanic: a reverse proxy cache that enforces cache policy set by the origin.

The goal is to make concrete what is often treated as magic:

> A CDN is a reverse proxy cache placed at the right position, with a control plane, routing, observability, and cache policy discipline.

## Key Architectural Decisions

**Nginx as Edge, Spring Boot as Origin**: Chosen for clarity of separation. Nginx speaks HTTP caching natively. Spring Boot owns business logic and cache policy headers. Neither knows the other's internal implementation.

**Cache policy owned by Origin**: Origin emits `Cache-Control`. Edge respects it. This is the correct model — Origin knows what is safe to cache and for how long. Edge should not make content-level decisions.

**Content hash in asset filenames**: Provides strong cache-busting without explicit purge infrastructure. The filename change is the invalidation signal.

## AI-Assisted Work

Initial Nginx configuration and Spring Boot controllers were reviewed with AI assistance. Cache policy rules and failure modes were validated against RFC 7234 semantics.

Known AI mistake pattern: AI tends to suggest overly aggressive cache TTLs for API responses without accounting for data freshness requirements. All TTL values in this module were validated against the intended staleness tolerance.

## Open Questions

- What is the right stale-while-revalidate window for `/api/products/{id}` in production?
- Should cache partitioning by user be done at Edge (cache key) or at Origin (Vary header)?
- At what request rate does `proxy_cache_lock` become a bottleneck?
