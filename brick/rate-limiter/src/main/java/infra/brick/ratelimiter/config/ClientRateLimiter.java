package infra.brick.ratelimiter.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class ClientRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final RateLimiterProperties properties;
    private final Clock clock;
    private final AtomicLong allowedRequests = new AtomicLong();
    private final AtomicLong rejectedRequests = new AtomicLong();
    private final AtomicLong bucketsCreated = new AtomicLong();

    ClientRateLimiter(RateLimiterProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    RateLimitDecision tryAcquire(String clientId) {
        Instant now = clock.instant();
        evictExpiredBuckets(now);

        TokenBucket bucket = buckets.computeIfAbsent(clientId, ignored -> {
            bucketsCreated.incrementAndGet();
            return new TokenBucket(properties.capacity(), now);
        });

        RateLimitDecision decision = bucket.tryConsume(now, properties);
        if (decision.allowed()) {
            allowedRequests.incrementAndGet();
        } else {
            rejectedRequests.incrementAndGet();
        }
        return decision;
    }

    int activeBuckets() {
        return buckets.size();
    }

    long allowedRequests() {
        return allowedRequests.get();
    }

    long rejectedRequests() {
        return rejectedRequests.get();
    }

    long bucketsCreated() {
        return bucketsCreated.get();
    }

    private void evictExpiredBuckets(Instant now) {
        Duration bucketTtl = properties.bucketTtl();
        Iterator<Map.Entry<String, TokenBucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            TokenBucket bucket = iterator.next().getValue();
            if (bucket.isIdleSince(now, bucketTtl)) {
                iterator.remove();
            }
        }
    }

    private static final class TokenBucket {

        private int tokens;
        private Instant lastRefillAt;
        private Instant lastSeenAt;

        private TokenBucket(int capacity, Instant now) {
            this.tokens = capacity;
            this.lastRefillAt = now;
            this.lastSeenAt = now;
        }

        private synchronized RateLimitDecision tryConsume(Instant now, RateLimiterProperties properties) {
            refill(now, properties);
            lastSeenAt = now;

            boolean allowed = tokens > 0;
            if (allowed) {
                tokens--;
            }

            return new RateLimitDecision(
                    allowed,
                    properties.capacity(),
                    tokens,
                    nextResetAt(now, properties)
            );
        }

        private synchronized boolean isIdleSince(Instant now, Duration bucketTtl) {
            return lastSeenAt.plus(bucketTtl).isBefore(now);
        }

        private void refill(Instant now, RateLimiterProperties properties) {
            Duration refillPeriod = properties.refillPeriod();
            long elapsedMillis = Duration.between(lastRefillAt, now).toMillis();
            long periods = elapsedMillis / refillPeriod.toMillis();
            if (periods <= 0) {
                return;
            }

            long refillAmount = periods * properties.refillTokens();
            tokens = Math.min(properties.capacity(), Math.toIntExact(Math.min(Integer.MAX_VALUE, tokens + refillAmount)));
            lastRefillAt = lastRefillAt.plus(refillPeriod.multipliedBy(periods));
        }

        private Instant nextResetAt(Instant now, RateLimiterProperties properties) {
            if (tokens > 0) {
                return now;
            }
            return lastRefillAt.plus(properties.refillPeriod());
        }
    }
}
