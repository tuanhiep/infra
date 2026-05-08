package infra.brick.circuitbreaker.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import infra.brick.circuitbreaker.config.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PaymentQuoteServiceTest {

    private final Queue<ExecutorService> executors = new ArrayDeque<>();

    @AfterEach
    void shutdownExecutors() {
        executors.forEach(ExecutorService::shutdownNow);
    }

    @Test
    void openCircuitDoesNotCallRemoteAgain() {
        CountingGateway gateway = new CountingGateway(request -> {
            throw new RemotePaymentGatewayException("downstream unavailable");
        });
        TestFixture fixture = fixture(gateway, defaultProperties(), defaultConfig());
        PaymentQuoteRequest request = request();

        fixture.service().quote(request);
        fixture.service().quote(request);

        PaymentQuoteResponse blockedByOpenCircuit = fixture.service().quote(request);

        assertThat(blockedByOpenCircuit.degraded()).isTrue();
        assertThat(blockedByOpenCircuit.reason()).contains("circuit breaker is open");
        assertThat(gateway.calls()).isEqualTo(2);
        assertThat(fixture.circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void returnsCachedFallbackAfterSuccessfulQuoteThenDownstreamFailure() {
        QueueGateway gateway = new QueueGateway(List.of(
                request -> quoteFromGateway(request, "3.20"),
                request -> {
                    throw new RemotePaymentGatewayException("temporary timeout");
                }
        ));
        TestFixture fixture = fixture(gateway, defaultProperties(), defaultConfig());
        PaymentQuoteRequest request = request();

        PaymentQuoteResponse live = fixture.service().quote(request);
        PaymentQuoteResponse fallback = fixture.service().quote(request);

        assertThat(live.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(fallback.source()).isEqualTo(QuoteSource.CACHE);
        assertThat(fallback.networkFee()).isEqualByComparingTo(live.networkFee());
        assertThat(fallback.degraded()).isTrue();
    }

    @Test
    void returnsConservativeDefaultWhenNoCacheExists() {
        CountingGateway gateway = new CountingGateway(request -> {
            throw new RemotePaymentGatewayException("downstream unavailable");
        });
        TestFixture fixture = fixture(gateway, defaultProperties(), defaultConfig());

        PaymentQuoteResponse response = fixture.service().quote(request());

        assertThat(response.source()).isEqualTo(QuoteSource.CONSERVATIVE_DEFAULT);
        assertThat(response.networkFee()).isEqualByComparingTo("3.5000");
        assertThat(response.degraded()).isTrue();
    }

    @Test
    void doesNotUseExpiredLastKnownGoodQuote() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-07T10:00:00Z"));
        QueueGateway gateway = new QueueGateway(List.of(
                request -> quoteFromGateway(request, "3.20"),
                request -> {
                    throw new RemotePaymentGatewayException("downstream unavailable");
                }
        ));
        CircuitBreakerProperties properties = defaultProperties(Duration.ofMillis(500), Duration.ofSeconds(5));
        TestFixture fixture = fixture(gateway, properties, defaultConfig(), clock);

        PaymentQuoteResponse live = fixture.service().quote(request());
        clock.advance(Duration.ofSeconds(6));
        PaymentQuoteResponse fallback = fixture.service().quote(request());

        assertThat(live.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(fallback.source()).isEqualTo(QuoteSource.CONSERVATIVE_DEFAULT);
        assertThat(fallback.networkFee()).isNotEqualByComparingTo(live.networkFee());
    }

    @Test
    void businessRuleExceptionIsNotCountedAsDownstreamFailure() {
        CountingGateway gateway = new CountingGateway(request -> {
            throw new BusinessRuleException("card type is not eligible");
        });
        TestFixture fixture = fixture(gateway, defaultProperties(), defaultConfig());

        assertThatThrownBy(() -> fixture.service().quote(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not eligible");

        assertThat(fixture.circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(fixture.circuitBreaker().getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(fixture.circuitBreaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void programmingErrorPropagatesAndIsNotConvertedToFallback() {
        CountingGateway gateway = new CountingGateway(request -> {
            throw new IllegalStateException("bad gateway client configuration");
        });
        TestFixture fixture = fixture(gateway, defaultProperties(), defaultConfig());

        assertThatThrownBy(() -> fixture.service().quote(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bad gateway client configuration");

        assertThat(fixture.circuitBreaker().getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    @Test
    void halfOpenRecoveryClosesCircuitAfterSuccessfulProbe() throws InterruptedException {
        QueueGateway gateway = new QueueGateway(List.of(
                request -> {
                    throw new RemotePaymentGatewayException("first failure");
                },
                request -> {
                    throw new RemotePaymentGatewayException("second failure");
                },
                request -> quoteFromGateway(request, "3.20")
        ));
        CircuitBreakerConfig config = config(Duration.ofMillis(50), Duration.ofMillis(500));
        TestFixture fixture = fixture(gateway, defaultProperties(), config);

        fixture.service().quote(request());
        fixture.service().quote(request());
        assertThat(fixture.circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(75);
        PaymentQuoteResponse probe = fixture.service().quote(request());

        assertThat(probe.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(fixture.circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void timeoutIsTreatedAsDownstreamFailureAndUsesFallback() {
        CountingGateway gateway = new CountingGateway(request -> {
            Thread.sleep(100);
            return quoteFromGateway(request, "3.20");
        });
        CircuitBreakerProperties properties = defaultProperties(Duration.ofMillis(20), Duration.ofMinutes(5));
        TestFixture fixture = fixture(gateway, properties, defaultConfig());

        PaymentQuoteResponse response = fixture.service().quote(request());

        assertThat(response.source()).isEqualTo(QuoteSource.CONSERVATIVE_DEFAULT);
        assertThat(response.reason()).contains("timed out");
        assertThat(fixture.circuitBreaker().getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void slowSuccessfulCallsCanOpenCircuitWhenSlowCallRateThresholdIsReached() {
        CountingGateway gateway = new CountingGateway(request -> {
            Thread.sleep(40);
            return quoteFromGateway(request, "3.20");
        });
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(100)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofMillis(20))
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordExceptions(RemotePaymentGatewayException.class)
                .ignoreExceptions(BusinessRuleException.class, IllegalArgumentException.class, IllegalStateException.class)
                .build();
        TestFixture fixture = fixture(gateway, defaultProperties(Duration.ofMillis(500), Duration.ofMinutes(5)), config);

        PaymentQuoteResponse first = fixture.service().quote(request());
        PaymentQuoteResponse second = fixture.service().quote(request());

        assertThat(first.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(second.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(fixture.circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(fixture.circuitBreaker().getMetrics().getSlowCallRate()).isEqualTo(100);
    }

    private TestFixture fixture(
            RemotePaymentGatewayClient gateway,
            CircuitBreakerProperties properties,
            CircuitBreakerConfig config
    ) {
        return fixture(gateway, properties, config, new MutableClock(Instant.parse("2026-05-07T10:00:00Z")));
    }

    private TestFixture fixture(
            RemotePaymentGatewayClient gateway,
            CircuitBreakerProperties properties,
            CircuitBreakerConfig config,
            Clock clock
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executors.add(executor);
        CircuitBreaker circuitBreaker = CircuitBreaker.of("payment-gateway-test", config);
        PaymentQuoteService service = new PaymentQuoteService(
                gateway,
                new FallbackQuoteCache(properties.fallbackCacheTtl(), clock),
                circuitBreaker,
                executor,
                properties
        );
        return new TestFixture(service, circuitBreaker);
    }

    private static CircuitBreakerConfig defaultConfig() {
        return config(Duration.ofMinutes(1), Duration.ofMillis(500));
    }

    private static CircuitBreakerConfig config(Duration waitDurationInOpenState, Duration slowCallDurationThreshold) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(slowCallDurationThreshold)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordExceptions(RemotePaymentGatewayException.class)
                .ignoreExceptions(BusinessRuleException.class, IllegalArgumentException.class, IllegalStateException.class)
                .build();
    }

    private static CircuitBreakerProperties defaultProperties() {
        return defaultProperties(Duration.ofMillis(500), Duration.ofMinutes(5));
    }

    private static CircuitBreakerProperties defaultProperties(Duration remoteCallTimeout, Duration fallbackCacheTtl) {
        return new CircuitBreakerProperties(
                "payment-gateway-test",
                50,
                50,
                Duration.ofMillis(500),
                2,
                2,
                1,
                Duration.ofMinutes(1),
                remoteCallTimeout,
                fallbackCacheTtl,
                "http://localhost:9090",
                Duration.ofMillis(100),
                Duration.ofMillis(200)
        );
    }

    private static PaymentQuoteRequest request() {
        return new PaymentQuoteRequest(new BigDecimal("100.00"), "USD");
    }

    private static PaymentQuoteResponse quoteFromGateway(PaymentQuoteRequest request, String fee) {
        return new PaymentQuoteResponse(
                request.amount(),
                request.currency(),
                new BigDecimal(fee),
                QuoteSource.PAYMENT_GATEWAY,
                false,
                "live quote",
                Instant.now()
        );
    }

    private record TestFixture(PaymentQuoteService service, CircuitBreaker circuitBreaker) {
    }

    private interface GatewayBehavior {

        PaymentQuoteResponse quote(PaymentQuoteRequest request) throws Exception;
    }

    private static class CountingGateway implements RemotePaymentGatewayClient {

        private final GatewayBehavior behavior;
        private int calls;

        CountingGateway(GatewayBehavior behavior) {
            this.behavior = behavior;
        }

        @Override
        public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
            calls++;
            try {
                return behavior.quote(request);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RemotePaymentGatewayException("gateway behavior failed", exception);
            }
        }

        int calls() {
            return calls;
        }
    }

    private static class QueueGateway implements RemotePaymentGatewayClient {

        private final Queue<GatewayBehavior> behaviors;

        QueueGateway(List<GatewayBehavior> behaviors) {
            this.behaviors = new ArrayDeque<>(behaviors);
        }

        @Override
        public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
            GatewayBehavior behavior = behaviors.remove();
            try {
                return behavior.quote(request);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RemotePaymentGatewayException("gateway behavior failed", exception);
            }
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
