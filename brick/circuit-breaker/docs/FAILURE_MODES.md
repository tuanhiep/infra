# Failure Modes - Circuit Breaker

## FM-01: Catch-All Fallback Masks Business Errors

**Trigger**: Fallback catches `Exception` instead of `RemotePaymentGatewayException`.

**Impact**: `BusinessRuleException` (invalid amount, rejected request) is silenced. Client receives a degraded quote instead of HTTP 400. Business validation is bypassed.

**Detection**: Client never sees 400 errors; degraded quote rate is unexpectedly high.

**Mitigation**: Fallback must only apply to known infrastructure failure types.

---

## FM-02: Fallback Cache Without TTL

**Trigger**: `FallbackQuoteCache` has no expiry. Stale quote from days ago is served.

**Impact**: Financial decisions made on outdated exchange rate or fee. Compliance risk.

**Detection**: Quote values unchanged over long periods; mismatch with market rate.

**Mitigation**: TTL per currency must be shorter than acceptable staleness window. Current default: 5 minutes.

---

## FM-03: Circuit Opens on Single Transient Failure

**Trigger**: `minimum-number-of-calls` is set to 1. One transient 503 opens the circuit.

**Impact**: All subsequent requests receive degraded fallback even though gateway is healthy.

**Detection**: Circuit state fluctuates rapidly; not-permitted count spikes without sustained failure.

**Mitigation**: Set `minimum-number-of-calls` ≥ 5 to require statistically meaningful failure sample.

---

## FM-04: Timeout Inconsistency

**Trigger**: `read-timeout: 5s` but `remote-call-timeout: 300ms`. Service budget cancels the Future at 300ms but the HTTP socket may remain open.

**Impact**: Connection pool exhaustion under sustained gateway slowness. Thread starvation.

**Detection**: Connection pool metrics show high utilization; thread dumps show blocked threads.

**Mitigation**: HTTP timeouts must be shorter than service-level timeout. All three layers must be coherent.

---

## FM-05: No Bulkhead — Thread Starvation

**Trigger**: Gateway consistently slow (200-250ms). Many concurrent requests fill thread pool.

**Impact**: Slow calls consume all threads. Other unrelated operations are blocked.

**Detection**: Thread pool saturation metrics; latency increase across all endpoints.

**Mitigation**: Add bulkhead (separate thread pool or semaphore) for gateway calls. Not yet implemented.

---

## FM-06: Conservative Default in Production Without Review

**Trigger**: Gateway fails. Conservative default `max(amount * 3.5%, 1.00)` is served.

**Impact**: Fee shown to customer may not match contractual rates. Legal and financial compliance risk.

**Detection**: Discrepancy between quoted fee and actual settlement fee.

**Mitigation**: Conservative default must be reviewed with legal and finance before enabling in production.
