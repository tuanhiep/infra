package com.tuanhiep.payment;

import java.math.BigDecimal;

public record PaymentQuoteRequest(BigDecimal amount, String currency) {

    public PaymentQuoteRequest {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new BusinessRuleException("currency is required");
        }
        currency = currency.toUpperCase();
    }
}
