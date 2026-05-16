# Design Doc - CDN Edge Cache

## Problem

Static and semi-static content served directly from an application origin creates unnecessary latency and load. Without an edge caching layer, every request hits the origin regardless of whether the response has changed.

## Goals

- Demonstrate correct Edge/Origin separation with Nginx as a reverse-proxy cache.
- Enforce correct cache policy per resource type (static assets, semi-static API, mutations).
- Make cache behavior observable via `X-Cache-Status` header.
- Simulate a WAF sample rule at the edge.

## Non-Goals

- Replace a production CDN (Cloudflare, Akamai, Fastly).
- Implement a distributed cache across multiple Edge nodes.
- Implement cache purge/invalidation API.
- Implement Edge Worker logic (JWT verification, A/B testing).

## Architecture

```text
Client
  └─▶ Nginx Edge :8081       (cache, rate-limit, WAF sample)
        └─▶ Spring Boot :8080 (origin: business logic, cache policy headers)
```

Edge responsibilities:

- Accept/reject requests based on WAF rules.
- Look up cache by key.
- On HIT: serve from `/var/cache/nginx`.
- On MISS: proxy to origin, cache if policy allows.

Origin responsibilities:

- Emit correct `Cache-Control` headers per resource type.
- Never trust Edge to enforce business logic.
- Forward client identity via `X-Forwarded-For`.

## Cache Policy

| Resource | Cache-Control | TTL | Notes |
|---|---|---|---|
| `/assets/**` (hashed) | `max-age=31536000, immutable` | 1 year | Content hash in filename |
| `/api/products/{id}` | `max-age=30, stale-while-revalidate=10` | 30s | Accept short staleness |
| `POST /api/checkout` | `no-store` | none | Mutations never cached |

## Cache Key

```
scheme + host + request_uri + accept_encoding
```

Note: Authorization header must bypass cache to prevent cross-user data leaks.

## Trade-offs

| Decision | Alternative | Reason |
|---|---|---|
| Nginx as Edge | Varnish, Squid | Simplicity, widely understood |
| In-process cache | Redis/Memcached | Demo scope; production needs shared cache |
| TTL-based invalidation | Purge API | Simpler; production needs explicit purge |
| Content hash in filename | ETag-based revalidation | Stronger guarantee, no conditional requests |

## Production Gaps

- No cache purge/invalidation control plane.
- No `proxy_cache_lock` — thundering herd risk under high concurrency.
- No Authorization-based cache bypass — potential data leak in multi-user scenarios.
- No multi-instance Origin cache consistency strategy.
- No OpenTelemetry correlation from Edge to Origin.
- No Prometheus metrics from Nginx.
