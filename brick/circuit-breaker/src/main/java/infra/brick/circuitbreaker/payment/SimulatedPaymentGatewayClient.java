package infra.brick.circuitbreaker.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
class SimulatedPaymentGatewayClient implements RemotePaymentGatewayClient {

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
        int call = sequence.incrementAndGet();
        if (call % 5 == 0 || call % 7 == 0) {
            throw new RemotePaymentGatewayException("simulated 503 from external payment gateway");
        }

        BigDecimal networkFee = request.amount().multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
        return new PaymentQuoteResponse(
                request.amount(),
                request.currency(),
                networkFee,
                QuoteSource.PAYMENT_GATEWAY,
                false,
                "live quote",
                Instant.now()
        );
    }
}
