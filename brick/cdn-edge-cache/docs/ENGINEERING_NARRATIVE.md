# Engineering Narrative - CDN Edge Cache

## The Problem Worth Solving

Most engineers treat CDN as a checkbox: "we use Cloudflare." This module makes the mechanic explicit. It answers: what exactly does an edge cache do, what must the origin control, and where do the failure modes live?

## What Was Built

A working Edge/Origin stack: Nginx as the cache layer, Spring Boot as the origin. Three resource types with distinct cache policies: immutable static assets with content-hashed filenames, semi-static API responses with short TTL and stale-while-revalidate, and mutations that must never be cached.

Cache behavior is observable via `X-Cache-Status`. A WAF sample rule demonstrates edge-level request filtering.

## The Judgment Behind It

The key design decision: cache policy belongs to the origin, not the edge. The origin knows what is safe to cache and for how long. The edge enforces the policy but does not interpret content.

This separation matters in production. When cache behavior is wrong, you know exactly where to look: if the policy is wrong, fix the origin headers; if the enforcement is wrong, fix the edge config.

Content-hash-in-filename for static assets was chosen over ETag-based revalidation because it eliminates conditional requests entirely. The filename change is the invalidation signal — no purge API needed for this class of assets.

## What This Demonstrates

- Edge/Origin separation as a first-class design concern.
- Cache policy as an origin responsibility, not an edge decision.
- Failure mode awareness: cache poisoning, thundering herd, data leak via cached authenticated responses.
- The difference between what this demo handles and what production requires.

## Production Follow-Up

A production CDN layer would add: purge/invalidation control plane, surrogate keys for bulk invalidation, cache partitioning per user identity, Edge Worker for auth token verification, OpenTelemetry correlation from Edge to Origin, and Nginx Prometheus metrics.

This module is the foundation that makes those follow-up decisions meaningful.
