package infra.brick.ratelimiter.config;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class RateLimiterConfiguration implements WebMvcConfigurer {

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RESET_HEADER = "X-RateLimit-Reset";

    private final ClientRateLimiter rateLimiter;
    private final RateLimiterProperties properties;

    RateLimiterConfiguration(RateLimiterProperties properties) {
        this.rateLimiter = new ClientRateLimiter(properties, Clock.systemUTC());
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor()).addPathPatterns("/api/**");
    }

    @Bean
    public HandlerInterceptor rateLimitingInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                RateLimitDecision decision = rateLimiter.tryAcquire(clientId(request));
                response.setHeader(LIMIT_HEADER, String.valueOf(decision.limit()));
                response.setHeader(REMAINING_HEADER, String.valueOf(decision.remaining()));
                response.setHeader(RESET_HEADER, String.valueOf(decision.resetAt().getEpochSecond()));

                if (!decision.allowed()) {
                    response.setStatus(429);
                }
                return decision.allowed();
            }
        };
    }

    @Bean
    public MeterBinder rateLimiterMetrics() {
        return registry -> {
            Gauge.builder("rate.limiter.active.buckets", rateLimiter, ClientRateLimiter::activeBuckets)
                    .tag("name", properties.name())
                    .register(registry);
            FunctionCounter.builder("rate.limiter.allowed.requests", rateLimiter, ClientRateLimiter::allowedRequests)
                    .tag("name", properties.name())
                    .register(registry);
            FunctionCounter.builder("rate.limiter.rejected.requests", rateLimiter, ClientRateLimiter::rejectedRequests)
                    .tag("name", properties.name())
                    .register(registry);
            FunctionCounter.builder("rate.limiter.buckets.created", rateLimiter, ClientRateLimiter::bucketsCreated)
                    .tag("name", properties.name())
                    .register(registry);
        };
    }

    private String clientId(HttpServletRequest request) {
        String headerValue = request.getHeader(properties.clientIdHeader());
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        return request.getRemoteAddr();
    }
}
