package infra.brick.circuitbreaker.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentQuoteResponse(
        BigDecimal amount,
        String currency,
        BigDecimal networkFee,
        QuoteSource source,
        boolean degraded,
        String reason,
        Instant quotedAt
) {

    PaymentQuoteResponse asDegraded(String reason, java.time.Clock clock) {
        return new PaymentQuoteResponse(
                amount,
                currency,
                networkFee,
                QuoteSource.CACHE,
                true,
                reason,
                Instant.now(clock)
        );
    }
}
