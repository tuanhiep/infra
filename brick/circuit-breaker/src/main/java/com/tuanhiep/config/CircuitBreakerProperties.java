package com.tuanhiep.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "infra.circuit-breaker.payment-gateway")
public record CircuitBreakerProperties(
        String name,
        float failureRateThreshold,
        float slowCallRateThreshold,
        Duration slowCallDurationThreshold,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        int permittedCallsInHalfOpenState,
        Duration waitDurationInOpenState
) {
}
