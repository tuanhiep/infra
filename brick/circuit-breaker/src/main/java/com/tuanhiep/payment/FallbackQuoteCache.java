package com.tuanhiep.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class FallbackQuoteCache {

    private final Map<String, PaymentQuoteResponse> lastKnownGoodQuotes = new ConcurrentHashMap<>();

    void remember(PaymentQuoteResponse response) {
        lastKnownGoodQuotes.put(response.currency(), response);
    }

    PaymentQuoteResponse degradedQuote(PaymentQuoteRequest request, String reason) {
        return Optional.ofNullable(lastKnownGoodQuotes.get(request.currency()))
                .map(cached -> cached.asDegraded(reason))
                .orElseGet(() -> conservativeDefault(request, reason));
    }

    private PaymentQuoteResponse conservativeDefault(PaymentQuoteRequest request, String reason) {
        BigDecimal fee = request.amount().multiply(new BigDecimal("0.035")).max(new BigDecimal("1.00"));
        return new PaymentQuoteResponse(
                request.amount(),
                request.currency(),
                fee,
                QuoteSource.CONSERVATIVE_DEFAULT,
                true,
                reason,
                Instant.now()
        );
    }
}
