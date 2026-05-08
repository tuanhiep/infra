package infra.brick.circuitbreaker.payment;

public interface RemotePaymentGatewayClient {

    PaymentQuoteResponse quote(PaymentQuoteRequest request);
}
