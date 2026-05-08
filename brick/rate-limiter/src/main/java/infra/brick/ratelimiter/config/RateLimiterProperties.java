package infra.brick.ratelimiter.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("infra.brick.rate-limiter")
public record RateLimiterProperties(
        String name,
        int capacity,
        int refillTokens,
        Duration refillPeriod,
        Duration bucketTtl,
        String clientIdHeader
) {

    public RateLimiterProperties {
        requireText(name, "name");
        requirePositive(capacity, "capacity");
        requirePositive(refillTokens, "refillTokens");
        requirePositive(refillPeriod, "refillPeriod");
        requirePositive(bucketTtl, "bucketTtl");
        requireText(clientIdHeader, "clientIdHeader");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }

    private static void requirePositive(Duration value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
