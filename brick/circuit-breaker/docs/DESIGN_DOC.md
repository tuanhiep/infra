# Design Doc - Circuit Breaker

## Problem

Outbound dependencies fail in multiple ways: transient errors, sustained degradation, slow responses that hold threads, and business-level rejections. Treating all failure modes the same leads to either masking real bugs (catch-all fallback) or failing hard when graceful degradation is possible.

## Goals

- Demonstrate correct failure taxonomy: distinguish infrastructure failure, business failure, and programming error.
- Implement ordered fallback: last-known-good cache → conservative default → fail closed.
- Layer timeouts correctly: connect timeout, read timeout, service-level timeout.
- Expose circuit state and metrics for observability.

## Non-Goals

- Replace a production resilience framework.
- Implement retry logic (requires idempotency contract on gateway).
- Implement bulkhead / thread pool isolation.
- Implement distributed fallback cache (multi-instance consistency).

## Architecture

```text
HTTP request
  └─▶ PaymentQuoteController
        └─▶ PaymentQuoteService  (owns fallback domain)
              └─▶ Resilience4j CircuitBreaker
                    └─▶ HttpPaymentGatewayClient  (Spring RestClient, real HTTP)
                          └─▶ Payment Gateway (configurable base URL)
```

## Failure Taxonomy

| Exception | Type | Breaker | Response |
|---|---|---|---|
| `RemotePaymentGatewayException` | Infrastructure | Records failure | Ordered fallback |
| `CallNotPermittedException` | Circuit open | Not-permitted count | Ordered fallback, no remote call |
| `BusinessRuleException` | Business | Ignored | Propagate HTTP 400 |
| `IllegalArgumentException/StateException` | Programming | Ignored | Propagate, no fallback |
| Slow success | Infrastructure | Records slow call | Success; may open circuit |

## Fallback Strategy

1. Last-known-good cache per currency (TTL: 5 minutes).
2. Conservative default: `max(amount * 3.5%, 1.00)`.
3. Programming or business error: propagate unchanged.

## Timeout Layering

```
connect-timeout: 200ms   — socket connection phase
read-timeout: 250ms      — response body phase
remote-call-timeout: 300ms — service-level budget
```

All three must be consistent with upstream gateway SLO.

## Trade-offs

| Decision | Alternative | Reason |
|---|---|---|
| Resilience4j | Hystrix, custom | Active maintenance, Spring native integration |
| Count-based sliding window | Time-based | Deterministic for testing |
| Per-currency cache TTL | Single TTL | Different currencies may have different volatility |
| Conservative default | Hard fail | Allows business to continue degraded |

## Production Gaps

- Fallback cache is in-memory only; multi-instance deployments share no state.
- Conservative default fee requires legal/financial review before production use.
- No retry policy; safe retry requires idempotency guarantee from gateway.
- No bulkhead; sustained gateway slowness can exhaust thread pool.
- Alert thresholds, dashboard, and runbook are not implemented.
- OpenTelemetry distributed tracing is not implemented.
