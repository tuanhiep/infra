package infra.brick.circuitbreaker.payment;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class PaymentQuoteService {

    private final RemotePaymentGatewayClient remoteClient;
    private final FallbackQuoteCache fallbackCache;
    private final CircuitBreaker circuitBreaker;
    private final ExecutorService remoteCallExecutor;
    private final Duration remoteCallTimeout;

    public PaymentQuoteService(
            RemotePaymentGatewayClient remoteClient,
            FallbackQuoteCache fallbackCache,
            CircuitBreaker circuitBreaker,
            ExecutorService remoteCallExecutor,
            infra.brick.circuitbreaker.config.CircuitBreakerProperties properties
    ) {
        this.remoteClient = remoteClient;
        this.fallbackCache = fallbackCache;
        this.circuitBreaker = circuitBreaker;
        this.remoteCallExecutor = remoteCallExecutor;
        this.remoteCallTimeout = properties.remoteCallTimeout();
    }

    public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
        Supplier<PaymentQuoteResponse> protectedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> callRemoteWithTimeout(request)
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
        }
    }

    private PaymentQuoteResponse callRemoteWithTimeout(PaymentQuoteRequest request) {
        Future<PaymentQuoteResponse> future = remoteCallExecutor.submit(() -> remoteClient.quote(request));

        try {
            return future.get(remoteCallTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new RemotePaymentGatewayException("timed out after " + remoteCallTimeout, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RemotePaymentGatewayException("interrupted while waiting for payment gateway", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RemotePaymentGatewayException("payment gateway failed with checked exception", cause);
        }
    }
}
