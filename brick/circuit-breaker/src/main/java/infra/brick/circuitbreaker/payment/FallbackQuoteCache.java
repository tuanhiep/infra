package infra.brick.circuitbreaker.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FallbackQuoteCache {

    private final Map<String, CacheEntry> lastKnownGoodQuotes = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public FallbackQuoteCache(infra.brick.circuitbreaker.config.CircuitBreakerProperties properties, Clock clock) {
        this(properties.fallbackCacheTtl(), clock);
    }

    FallbackQuoteCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    void remember(PaymentQuoteResponse response) {
        lastKnownGoodQuotes.put(response.currency(), new CacheEntry(response, Instant.now(clock)));
    }

    PaymentQuoteResponse degradedQuote(PaymentQuoteRequest request, String reason) {
        return Optional.ofNullable(lastKnownGoodQuotes.get(request.currency()))
                .filter(this::isFresh)
                .map(entry -> entry.response().asDegraded(reason, clock))
                .orElseGet(() -> conservativeDefault(request, reason));
    }

    private boolean isFresh(CacheEntry entry) {
        Instant expiresAt = entry.cachedAt().plus(ttl);
        return Instant.now(clock).isBefore(expiresAt);
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
                Instant.now(clock)
        );
    }

    private record CacheEntry(PaymentQuoteResponse response, Instant cachedAt) {
    }
}
