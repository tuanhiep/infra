package infra.brick.ratelimiter.config;

import java.time.Instant;

record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        Instant resetAt
) {}
