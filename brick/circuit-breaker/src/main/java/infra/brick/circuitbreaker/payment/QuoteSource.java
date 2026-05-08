package infra.brick.circuitbreaker.payment;

public enum QuoteSource {
    PAYMENT_GATEWAY,
    CACHE,
    CONSERVATIVE_DEFAULT
}
