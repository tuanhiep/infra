# Failure Modes - CDN Edge Cache

## FM-01: Cache Key Missing Dimension (Cache Poisoning)

**Trigger**: Cache key does not include `Accept-Encoding`. A gzip response is cached and served to a client that did not request gzip.

**Impact**: Garbled response for clients that do not support the cached encoding.

**Detection**: Client decoding errors; unexpected `Content-Encoding` in responses.

**Mitigation**: Include `$http_accept_encoding` in `proxy_cache_key`.

---

## FM-02: Authenticated Response Cached Without Bypass

**Trigger**: A response containing user-specific data is cached when `Authorization` header is present and no bypass rule exists.

**Impact**: User B receives User A's data from cache.

**Detection**: Cross-user data in responses; security audit.

**Mitigation**: `proxy_cache_bypass $http_authorization;` or partition cache key by identity.

---

## FM-03: Thundering Herd on Cache Miss

**Trigger**: Many concurrent requests miss cache for the same key. All requests proxy to Origin simultaneously.

**Impact**: Origin overload; potential cascade failure.

**Detection**: Spike in Origin request rate after cache expiry.

**Mitigation**: `proxy_cache_lock on; proxy_cache_lock_timeout 5s;`

---

## FM-04: Stale Static Asset After Deploy

**Trigger**: Deployed new asset with same filename. Old version cached for 1 year.

**Impact**: Users receive outdated JavaScript/CSS until cache expires.

**Detection**: Version mismatch between HTML references and cached files.

**Mitigation**: Always embed content hash in asset filename.

---

## FM-05: Origin Down, No Stale Fallback

**Trigger**: Origin is unavailable. `proxy_cache_use_stale` is not configured.

**Impact**: All requests return 502 even though valid cached responses exist.

**Detection**: 502 error rate spike while cache has valid entries.

**Mitigation**: `proxy_cache_use_stale error timeout updating http_502 http_503;`

---

## FM-06: WAF False Positive

**Trigger**: Legitimate request matches WAF block rule (e.g., product name contains SQL keyword).

**Impact**: Valid user requests blocked at Edge.

**Detection**: Client 403 errors; WAF block log analysis.

**Mitigation**: Tune WAF rules; implement allow-listing for known safe patterns.
