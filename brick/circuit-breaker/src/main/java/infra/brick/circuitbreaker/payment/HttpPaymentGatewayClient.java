package infra.brick.circuitbreaker.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class HttpPaymentGatewayClient implements RemotePaymentGatewayClient {

    private final RestClient restClient;
    private final Clock clock;

    HttpPaymentGatewayClient(RestClient paymentGatewayRestClient, Clock clock) {
        this.restClient = paymentGatewayRestClient;
        this.clock = clock;
    }

    @Override
    public PaymentQuoteResponse quote(PaymentQuoteRequest request) {
        try {
            GatewayQuoteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quotes")
                            .queryParam("amount", request.amount())
                            .queryParam("currency", request.currency())
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, httpResponse) -> {
                        throw new BusinessRuleException("payment gateway rejected quote request");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, httpResponse) -> {
                        throw new RemotePaymentGatewayException("payment gateway returned " + httpResponse.getStatusCode());
                    })
                    .requiredBody(GatewayQuoteResponse.class);

            return new PaymentQuoteResponse(
                    request.amount(),
                    request.currency(),
                    response.networkFee(),
                    QuoteSource.PAYMENT_GATEWAY,
                    false,
                    response.reason(),
                    Instant.now(clock)
            );
        } catch (BusinessRuleException | RemotePaymentGatewayException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new RemotePaymentGatewayException("payment gateway HTTP call failed", exception);
        }
    }

    private record GatewayQuoteResponse(BigDecimal networkFee, String reason) {
    }
}
