package com.tuanhiep.payment;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class PaymentQuoteService {

    private final RemotePaymentGatewayClient remoteClient;
    private final FallbackQuoteCache fallbackCache;
    private final CircuitBreaker circuitBreaker;

    public PaymentQuoteService(
            RemotePaymentGatewayClient remoteClient,
            FallbackQuoteCache fallbackCache,
            CircuitBreaker circuitBreaker
    ) {
        this.remoteClient = remoteClient;
        this.fallbackCache = fallbackCache;
        this.circuitBreaker = circuitBreaker;
    }

    public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
        Supplier<PaymentQuoteResponse> protectedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> remoteClient.quote(request)
        );

        try {
            PaymentQuoteResponse response = protectedCall.get();
            fallbackCache.remember(response);
            return response;
        } catch (CallNotPermittedException exception) {
            return fallbackCache.degradedQuote(request, "payment gateway circuit breaker is open");
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (RemotePaymentGatewayException exception) {
            return fallbackCache.degradedQuote(request, "payment gateway failed: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return fallbackCache.degradedQuote(request, "payment gateway failed unexpectedly");
        }
    }
}
