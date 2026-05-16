# Engineering Narrative - Circuit Breaker

## The Problem Worth Solving

Payment quote services depend on external gateways that fail in more than one way. The naive solution — catch all exceptions and return a default — is the most dangerous. It makes the system look healthy when code is broken, validation is failing, or configuration is wrong.

This module addresses the harder question: how do you fail gracefully without hiding real failures?

## What Was Built

A payment quote service with a Resilience4j circuit breaker protecting an outbound HTTP call to a payment gateway. Three fallback tiers: last-known-good cache per currency (5-minute TTL), conservative default fee, and explicit fail-closed for business and programming errors.

Timeouts are layered: connect timeout (200ms), read timeout (250ms), and service-level budget (300ms). The gateway client is a real Spring RestClient, not a mock interface.

Tests cover nine distinct scenarios: circuit open, cache fallback, conservative default, expired cache, business error propagation, programming error propagation, half-open recovery, timeout handling, and slow-call threshold.

## The Judgment Behind It

The key design decision is the failure taxonomy boundary. `RemotePaymentGatewayException` and `BusinessRuleException` must never be handled the same way. Infrastructure failures get a fallback because the operation may have been correct but the dependency was unavailable. Business failures get propagated because the operation itself was invalid — no amount of retrying or degrading changes that.

Catch-all fallback is the canonical anti-pattern here. It silences business validation errors, making them invisible to monitoring and to the client. The system looks healthy while quietly serving wrong data.

## What This Demonstrates

- Failure taxonomy as a first-class design concern.
- Fallback strategy with explicit degradation signal (`degraded=true`).
- Timeout layering consistency between HTTP and service-level budgets.
- Production gap awareness: conservative default requiring business approval, in-memory cache not suitable for multi-instance deployment.

## Production Follow-Up

Retry with idempotency guarantee, bulkhead for thread isolation, distributed fallback cache, alert rules and runbook, and OpenTelemetry distributed tracing from service layer to gateway.

This module is the foundation. The production follow-up is only meaningful once the failure taxonomy is right.
