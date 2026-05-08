# Circuit Breaker

Track: `brick`

Canonical Spring Boot brick for protecting outbound dependencies with a circuit breaker.

This module models a payment quote service calling an external payment gateway. The gateway is intentionally unreliable, and the application demonstrates the production boundary:

- domain service owns the fallback decision;
- Resilience4j owns circuit state and failure-rate accounting;
- configuration is externalized in `application.yml`;
- caller receives an explicit degraded response instead of a hidden exception;
- operators can inspect breaker state through a small control endpoint.

## Architecture

```text
HTTP request
  -> PaymentQuoteController
  -> PaymentQuoteService
  -> Resilience4j CircuitBreaker
  -> RemotePaymentGatewayClient
       success: remember last-known-good quote
       failure: return cache or conservative default
       open: skip remote call and return fallback immediately
```

## Run

```bash
mvn -pl brick/circuit-breaker spring-boot:run
```

Try the flow:

```bash
curl "http://localhost:8080/api/payment-quotes?amount=100.00&currency=USD"
curl "http://localhost:8080/api/circuit-breaker/payment-gateway"
curl "http://localhost:8080/actuator/health"
```

## Enterprise Rules Captured

- Put the breaker around outbound dependency calls, not around pure domain logic.
- Do not count business validation failures as downstream instability.
- Fallback is a product/domain contract: cache, conservative default, or fail closed.
- Open circuit means "protect the system now"; it should avoid making the remote call.
- Tune the sliding window from traffic volume, not from gut feeling.
- Expose state, failure rate, slow-call rate, and not-permitted calls for operations.

## Key Files

- `PaymentQuoteService`: boundary where the protected remote call and fallback live.
- `CircuitBreakerConfiguration`: explicit Resilience4j config for the payment gateway.
- `FallbackQuoteCache`: last-known-good fallback plus conservative default.
- `PaymentQuoteServiceTest`: verifies open circuit behavior and cached fallback behavior.

## Configuration

```yaml
infra:
  circuit-breaker:
    payment-gateway:
      failure-rate-threshold: 50
      slow-call-duration-threshold: 500ms
      sliding-window-size: 10
      minimum-number-of-calls: 5
      wait-duration-in-open-state: 10s
```

For a real service, pair this with:

- client-side timeout shorter than the upstream SLO;
- retry only for safe/idempotent operations;
- bulkhead for thread/connection isolation;
- alerting on sustained `OPEN` or high `notPermittedCalls`.
