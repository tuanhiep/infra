package infra.brick.ratelimiter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("infra.brick.rate-limiter")
public record RateLimiterProperties(
    String name,
    int limitForPeriod,
    Duration limitRefreshPeriod,
    Duration timeoutDuration
) {}
