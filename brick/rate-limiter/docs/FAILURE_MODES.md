# Failure Modes - Rate Limiter

## FM-01: Multiple Instances, No Shared State

**Trigger**: Application scaled to N instances. Each instance has its own in-memory bucket per client.

**Impact**: Effective rate limit is N × configured limit. A client sending requests round-robin can exceed quota by factor N.

**Detection**: Request throughput significantly exceeds configured limit; no 429 responses despite high request rate.

**Mitigation**: Shared distributed counter using Redis INCR + EXPIRE or atomic Lua script.

---

## FM-02: Race Condition in tryConsume

**Trigger**: Two concurrent threads call `tryConsume` for the same client when bucket has exactly 1 token. Both read `tokens=1`, both see ALLOWED, both decrement.

**Impact**: Bucket goes to -1. Quota exceeded by 1 per race.

**Detection**: Occasional quota over-run under high concurrency for same client.

**Mitigation**: `synchronized` block or atomic compare-and-swap on bucket state.

---

## FM-03: No Retry-After Header

**Trigger**: Client receives HTTP 429 with no `Retry-After` header. Client retries immediately in a loop.

**Impact**: 429 responses generate more load instead of reducing it. Thundering herd: rejected clients produce a flood of retry requests.

**Detection**: High request rate persists after 429 spike; client retry logic shows no backoff.

**Mitigation**: Include `Retry-After: <seconds-until-refill>` in all 429 responses.

---

## FM-04: Restart Resets All Quotas

**Trigger**: Application restarts (deploy, crash). All in-memory buckets are reset to full capacity.

**Impact**: Clients that consumed their quota regain it instantly after restart. Quota bypass via triggered restart if attacker can cause restarts.

**Detection**: Request rate spike immediately after each deployment.

**Mitigation**: Persist quota state to Redis with TTL. Or explicitly document that restart resets quotas and accept this behavior in SLA.

---

## FM-05: Clock Skew in Lazy Refill

**Trigger**: System clock adjusted backward (NTP correction). Lazy refill calculates `now - lastRefillTime` as negative.

**Impact**: Negative elapsed time → negative tokens accumulated → possible underflow or refill error.

**Detection**: Unexpected 429 responses after NTP correction; bucket shows abnormal token count.

**Mitigation**: Use monotonic clock (`System.nanoTime()`) instead of wall clock (`System.currentTimeMillis()`).

---

## FM-06: IP-Based Key Behind Shared NAT

**Trigger**: Many legitimate users behind a corporate NAT share the same source IP. Rate limit applies to the IP, not individual users.

**Impact**: One high-traffic user triggers 429 for all users behind the same NAT.

**Detection**: 429 responses affecting entire IP range simultaneously; no single user exceeds quota individually.

**Mitigation**: Use authentication-based key (API key, user ID) instead of IP. Add IP as secondary signal only.
