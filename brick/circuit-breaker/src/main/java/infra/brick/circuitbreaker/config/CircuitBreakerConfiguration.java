package infra.brick.circuitbreaker.config;

import infra.brick.circuitbreaker.payment.BusinessRuleException;
import infra.brick.circuitbreaker.payment.RemotePaymentGatewayException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

    @Bean
    RestClient paymentGatewayRestClient(CircuitBreakerProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    MeterBinder paymentGatewayCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        return registry -> {
            String name = circuitBreaker.getName();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            Gauge.builder("resilience4j.circuitbreaker.failure.rate", metrics, CircuitBreaker.Metrics::getFailureRate)
                    .tag("name", name)
                    .register(registry);
            Gauge.builder("resilience4j.circuitbreaker.slow.call.rate", metrics, CircuitBreaker.Metrics::getSlowCallRate)
                    .tag("name", name)
                    .register(registry);
            Gauge.builder("resilience4j.circuitbreaker.buffered.calls", metrics, CircuitBreaker.Metrics::getNumberOfBufferedCalls)
                    .tag("name", name)
                    .register(registry);
            Gauge.builder("resilience4j.circuitbreaker.failed.calls", metrics, CircuitBreaker.Metrics::getNumberOfFailedCalls)
                    .tag("name", name)
                    .register(registry);
            Gauge.builder(
                            "resilience4j.circuitbreaker.not.permitted.calls",
                            metrics,
                            CircuitBreaker.Metrics::getNumberOfNotPermittedCalls
                    )
                    .tag("name", name)
                    .register(registry);
            for (CircuitBreaker.State state : CircuitBreaker.State.values()) {
                Gauge.builder(
                                "resilience4j.circuitbreaker.state",
                                circuitBreaker,
                                breaker -> breaker.getState() == state ? 1 : 0
                        )
                        .tag("name", name)
                        .tag("state", state.name())
                        .register(registry);
            }
        };
    }

    @Bean
    AbstractHealthIndicator paymentGatewayHealthIndicator(CircuitBreaker circuitBreaker) {
        return new AbstractHealthIndicator() {
            @Override
            protected void doHealthCheck(Health.Builder builder) {
                CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                    builder.outOfService();
                } else {
                    builder.up();
                }
                builder.withDetail("name", circuitBreaker.getName())
                        .withDetail("state", circuitBreaker.getState().name())
                        .withDetail("failureRate", metrics.getFailureRate())
                        .withDetail("slowCallRate", metrics.getSlowCallRate())
                        .withDetail("bufferedCalls", metrics.getNumberOfBufferedCalls())
                        .withDetail("failedCalls", metrics.getNumberOfFailedCalls())
                        .withDetail("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            }
        };
    }
}
