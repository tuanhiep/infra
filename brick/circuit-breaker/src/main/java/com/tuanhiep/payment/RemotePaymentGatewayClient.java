package com.tuanhiep.payment;

public interface RemotePaymentGatewayClient {

    PaymentQuoteResponse quote(PaymentQuoteRequest request);
}
