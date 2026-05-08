package infra.brick.circuitbreaker.payment;

public class RemotePaymentGatewayException extends RuntimeException {

    public RemotePaymentGatewayException(String message) {
        super(message);
    }

    public RemotePaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
