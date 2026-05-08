package infra.brick.circuitbreaker.config;

import infra.brick.circuitbreaker.payment.BusinessRuleException;
import infra.brick.circuitbreaker.payment.RemotePaymentGatewayException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
                .recordExceptions(RemotePaymentGatewayException.class)
                .ignoreExceptions(BusinessRuleException.class, IllegalArgumentException.class, IllegalStateException.class)
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

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "shutdownNow")
    ExecutorService remotePaymentGatewayExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
