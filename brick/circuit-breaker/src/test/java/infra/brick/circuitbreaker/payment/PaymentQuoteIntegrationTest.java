package infra.brick.circuitbreaker.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoint.health.show-details=always",
                "infra.circuit-breaker.payment-gateway.failure-rate-threshold=50",
                "infra.circuit-breaker.payment-gateway.sliding-window-size=2",
                "infra.circuit-breaker.payment-gateway.minimum-number-of-calls=2",
                "infra.circuit-breaker.payment-gateway.wait-duration-in-open-state=5s",
                "infra.circuit-breaker.payment-gateway.remote-call-timeout=500ms",
                "infra.circuit-breaker.payment-gateway.connect-timeout=100ms",
                "infra.circuit-breaker.payment-gateway.read-timeout=400ms"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentQuoteIntegrationTest {

    private static HttpServer gateway;
    private static final AtomicInteger gatewayCalls = new AtomicInteger();
    private static volatile int gatewayStatus = 200;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        ensureGatewayStarted();
        registry.add("infra.circuit-breaker.payment-gateway.base-url", PaymentQuoteIntegrationTest::gatewayBaseUrl);
    }

    @BeforeEach
    void resetGateway() {
        gatewayStatus = 200;
        gatewayCalls.set(0);
    }

    @AfterAll
    static void stopGateway() {
        if (gateway != null) {
            gateway.stop(0);
        }
    }

    @Test
    void paymentQuoteEndpointCallsHttpGatewayAndExposesOperationalSignals() {
        ResponseEntity<PaymentQuoteResponse> quote = restTemplate().get()
                .uri(appUrl("/api/payment-quotes?amount=100.00&currency=USD"))
                .retrieve()
                .toEntity(PaymentQuoteResponse.class);

        assertThat(quote.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(quote.getBody()).isNotNull();
        assertThat(quote.getBody().source()).isEqualTo(QuoteSource.PAYMENT_GATEWAY);
        assertThat(quote.getBody().networkFee()).isEqualByComparingTo(new BigDecimal("3.20"));
        assertThat(quote.getBody().degraded()).isFalse();
        assertThat(gatewayCalls).hasValue(1);

        String breakerState = get("/api/circuit-breaker/payment-gateway");
        assertThat(breakerState).contains("\"state\":\"CLOSED\"");

        String health = get("/actuator/health");
        assertThat(health).contains("\"paymentGateway\"");
        assertThat(health).contains("\"status\":\"UP\"");

        String metric = get("/actuator/metrics/resilience4j.circuitbreaker.buffered.calls");
        assertThat(metric).contains("resilience4j.circuitbreaker.buffered.calls");
        assertThat(metric).contains("payment-gateway");

        String prometheus = get("/actuator/prometheus");
        assertThat(prometheus).contains("resilience4j_circuitbreaker_buffered_calls");
    }

    @Test
    void openCircuitHealthIsOutOfServiceAndDoesNotCallGatewayAgain() {
        gatewayStatus = 503;

        restTemplate().get()
                .uri(appUrl("/api/payment-quotes?amount=100.00&currency=USD"))
                .retrieve()
                .toEntity(PaymentQuoteResponse.class);
        restTemplate().get()
                .uri(appUrl("/api/payment-quotes?amount=100.00&currency=USD"))
                .retrieve()
                .toEntity(PaymentQuoteResponse.class);
        ResponseEntity<PaymentQuoteResponse> blocked = restTemplate().get()
                .uri(appUrl("/api/payment-quotes?amount=100.00&currency=USD"))
                .retrieve()
                .toEntity(PaymentQuoteResponse.class);

        assertThat(blocked.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(blocked.getBody()).isNotNull();
        assertThat(blocked.getBody().source()).isEqualTo(QuoteSource.CONSERVATIVE_DEFAULT);
        assertThat(blocked.getBody().reason()).contains("circuit breaker is open");
        assertThat(gatewayCalls).hasValue(2);

        ResponseEntity<String> health = getStringEntity("/actuator/health");
        assertThat(health.getStatusCode().value()).isEqualTo(503);
        assertThat(health.getBody()).contains("\"paymentGateway\"");
        assertThat(health.getBody()).contains("\"status\":\"OUT_OF_SERVICE\"");
    }

    private static synchronized void ensureGatewayStarted() {
        if (gateway != null) {
            return;
        }
        try {
            gateway = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            gateway.createContext("/quotes", PaymentQuoteIntegrationTest::handleQuote);
            gateway.start();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start test payment gateway", exception);
        }
    }

    private static String gatewayBaseUrl() {
        return "http://localhost:" + gateway.getAddress().getPort();
    }

    private static void handleQuote(HttpExchange exchange) throws IOException {
        gatewayCalls.incrementAndGet();
        if (gatewayStatus >= 500) {
            write(exchange, gatewayStatus, "{\"error\":\"gateway unavailable\"}");
            return;
        }
        write(exchange, 200, "{\"networkFee\":3.20,\"reason\":\"gateway quote\"}");
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private RestClient restTemplate() {
        return RestClient.create();
    }

    private String get(String path) {
        return restTemplate().get()
                .uri(appUrl(path))
                .retrieve()
                .requiredBody(String.class);
    }

    private ResponseEntity<String> getStringEntity(String path) {
        return restTemplate().get()
                .uri(appUrl(path))
                .exchange((request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    return ResponseEntity.status(response.getStatusCode()).body(body);
                });
    }

    private String appUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
