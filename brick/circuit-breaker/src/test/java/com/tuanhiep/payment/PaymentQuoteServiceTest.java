package com.tuanhiep.payment;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentQuoteServiceTest {

    @Test
    void opensCircuitAfterFailureThresholdAndUsesFallbackWithoutCallingRemoteAgain() {
        CountingGateway gateway = new CountingGateway(true);
        PaymentQuoteService service = service(gateway);
        PaymentQuoteRequest request = new PaymentQuoteRequest(new BigDecimal("100.00"), "USD");

        for (int i = 0; i < 4; i++) {
            PaymentQuoteResponse response = service.quote(request);
            assertThat(response.degraded()).isTrue();
        }

        PaymentQuoteResponse blockedByOpenCircuit = service.quote(request);

        assertThat(blockedByOpenCircuit.degraded()).isTrue();
        assertThat(blockedByOpenCircuit.reason()).contains("circuit breaker is open");
        assertThat(gateway.calls).isEqualTo(4);
    }

    @Test
    void returnsCachedFallbackAfterRemoteFailureWhenPreviousQuoteWasSuccessful() {
        FlakyGateway gateway = new FlakyGateway();
        PaymentQuoteService service = service(gateway);
        PaymentQuoteRequest request = new PaymentQuoteRequest(new BigDecimal("100.00"), "USD");

        PaymentQuoteResponse live = service.quote(request);
        PaymentQuoteResponse fallback = service.quote(request);

        assertThat(live.source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(fallback.source()).isEqualTo(QuoteSource.CACHE);
        assertThat(fallback.networkFee()).isEqualByComparingTo(live.networkFee());
    }

    private static PaymentQuoteService service(RemotePaymentGatewayClient gateway) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();

        return new PaymentQuoteService(
                gateway,
                new FallbackQuoteCache(),
                CircuitBreaker.of("payment-gateway-test", config)
        );
    }

    private static class CountingGateway implements RemotePaymentGatewayClient {

        private final boolean fail;
        private int calls;

        private CountingGateway(boolean fail) {
            this.fail = fail;
        }

        @Override
        public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
            calls++;
            if (fail) {
                throw new RemotePaymentGatewayException("downstream unavailable");
            }
            return quoteFromGateway(request);
        }
    }

    private static class FlakyGateway implements RemotePaymentGatewayClient {

        private int calls;

        @Override
        public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
            calls++;
            if (calls == 2) {
                throw new RemotePaymentGatewayException("temporary timeout");
            }
            return quoteFromGateway(request);
        }
    }

    private static PaymentQuoteResponse quoteFromGateway(PaymentQuoteRequest request) {
        return new PaymentQuoteResponse(
                request.amount(),
                request.currency(),
                new BigDecimal("3.20"),
                QuoteSource.PAYMENT_GATEWAY,
                false,
                "live quote",
                Instant.now()
        );
    }
}
