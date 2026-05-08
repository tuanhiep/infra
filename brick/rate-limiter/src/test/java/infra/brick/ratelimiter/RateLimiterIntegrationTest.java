package infra.brick.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimiterIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldAllowRequestWhenPermissionIsAvailable() {
        Response response = get("/api/time", "client-allow");

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.headers().getFirst("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.headers().getFirst("X-RateLimit-Remaining")).isEqualTo("4");
    }

    @Test
    void shouldDenyRequestWhenPermissionIsNotAvailable() {
        for (int i = 0; i < 5; i++) {
            assertThat(get("/api/time", "client-a").status().value()).isEqualTo(200);
        }

        Response response = get("/api/time", "client-a");

        assertThat(response.status().value()).isEqualTo(429);
        assertThat(response.headers().getFirst("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.headers().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.headers().getFirst("X-RateLimit-Reset")).isNotBlank();
    }

    @Test
    void shouldIsolateLimitsPerClient() {
        for (int i = 0; i < 5; i++) {
            assertThat(get("/api/time", "client-b").status().value()).isEqualTo(200);
        }

        Response response = get("/api/time", "client-c");

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.headers().getFirst("X-RateLimit-Remaining")).isEqualTo("4");
    }

    @Test
    void shouldExposeMetrics() {
        assertThat(get("/actuator/metrics/rate.limiter.active.buckets", "metrics-client").status().value())
                .isEqualTo(200);
        assertThat(get("/actuator/metrics/rate.limiter.allowed.requests", "metrics-client").status().value())
                .isEqualTo(200);
    }

    private Response get(String path, String clientId) {
        RestClient client = RestClient.create("http://localhost:" + port);
        return client.get()
                .uri(path)
                .header("X-Api-Key", clientId)
                .exchange((request, response) -> new Response(response.getStatusCode(), response.getHeaders()));
    }

    private record Response(HttpStatusCode status, HttpHeaders headers) {}
}
