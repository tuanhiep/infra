package com.tuanhiep.config;

import com.tuanhiep.payment.BusinessRuleException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CircuitBreakerConfiguration {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.failureRateThreshold())
                .slowCallRateThreshold(properties.slowCallRateThreshold())
                .slowCallDurationThreshold(properties.slowCallDurationThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(properties.permittedCallsInHalfOpenState())
                .waitDurationInOpenState(properties.waitDurationInOpenState())
                .ignoreExceptions(BusinessRuleException.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    CircuitBreaker paymentGatewayCircuitBreaker(
            CircuitBreakerRegistry registry,
            CircuitBreakerProperties properties
    ) {
        return registry.circuitBreaker(properties.name());
    }
}
