package infra.brick.circuitbreaker.payment;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentQuoteController {

    private final PaymentQuoteService service;
    private final CircuitBreaker circuitBreaker;

    PaymentQuoteController(PaymentQuoteService service, CircuitBreaker circuitBreaker) {
        this.service = service;
        this.circuitBreaker = circuitBreaker;
    }

    @GetMapping("/api/payment-quotes")
    PaymentQuoteResponse quote(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "USD") String currency
    ) {
        return service.quote(new PaymentQuoteRequest(amount, currency));
    }

    @GetMapping("/api/circuit-breaker/payment-gateway")
    CircuitBreakerSnapshot paymentGatewayState() {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        return new CircuitBreakerSnapshot(
                circuitBreaker.getName(),
                circuitBreaker.getState().name(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate(),
                metrics.getNumberOfBufferedCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getNumberOfNotPermittedCalls()
        );
    }

    record CircuitBreakerSnapshot(
            String name,
            String state,
            float failureRate,
            float slowCallRate,
            int bufferedCalls,
            int failedCalls,
            long notPermittedCalls
    ) {
    }
}
