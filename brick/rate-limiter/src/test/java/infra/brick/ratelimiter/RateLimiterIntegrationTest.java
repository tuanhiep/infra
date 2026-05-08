package infra.brick.ratelimiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestWhenPermissionIsAvailable() throws Exception {
        mockMvc.perform(get("/api/time"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDenyRequestWhenPermissionIsNotAvailable() throws Exception {
        // First 5 requests should be allowed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/time"))
                .andExpect(status().isOk());
        }

        // The 6th request should be denied
        mockMvc.perform(get("/api/time"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldExposeMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/resilience4j.ratelimiter.available.permissions"))
            .andExpect(status().isOk());
    }
}
