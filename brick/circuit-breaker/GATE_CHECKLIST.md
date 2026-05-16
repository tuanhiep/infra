# Gate Checklist - Circuit Breaker

## Correctness

- [x] Infrastructure failure (RemotePaymentGatewayException) triggers fallback.
- [x] Business failure (BusinessRuleException) propagates unchanged.
- [x] Programming error propagates, not silenced by fallback.
- [x] Circuit OPEN does not call remote gateway.
- [x] Fallback cache has TTL per currency.
- [x] Conservative default is explicit last resort, not catch-all.
- [x] Fallback response carries degraded=true and source.

## Architecture

- [x] Failure taxonomy is documented.
- [x] Fallback order is documented.
- [x] Timeout layers are documented and consistent.
- [x] Goals and non-goals are documented.
- [ ] Distributed cache strategy for multi-instance is documented.
- [ ] Retry policy decision is documented (awaiting idempotency contract).

## Implementation

- [x] Module wired into Maven reactor.
- [x] Spring Boot application starts.
- [x] HTTP API exists.
- [x] Real HTTP client with connect/read timeout.
- [x] Service tests cover 9 scenarios.
- [x] Integration tests cover HTTP end-to-end with real upstream mock.
- [x] Actuator health contributor exists.
- [x] Prometheus metrics export exists.
- [ ] proxy_cache_lock equivalent (bulkhead) not yet implemented.

## Operations

- [x] Actuator health endpoint reflects circuit state.
- [x] Custom metrics: failure rate, slow-call rate, buffered calls, not-permitted calls.
- [x] Prometheus export via /actuator/prometheus.
- [ ] Alert rules defined.
- [ ] Runbook exists.
- [ ] Dashboard exists.

## Failure

- [x] Initial failure modes are documented.
- [ ] Conservative default reviewed by finance/legal.
- [ ] Bulkhead for thread isolation implemented.
- [ ] Distributed cache consistency addressed.

## Narrative

- [x] README exists.
- [x] Design doc exists.
- [x] Engineering narrative exists.
- [ ] CV bullet drafted.
- [ ] Interview-ready 3-minute story rehearsed.
