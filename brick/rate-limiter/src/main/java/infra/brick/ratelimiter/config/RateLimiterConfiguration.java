package infra.brick.ratelimiter.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class RateLimiterConfiguration implements WebMvcConfigurer {

    private final RateLimiter rateLimiter;
    private final RateLimiterProperties properties;

    RateLimiterConfiguration(RateLimiterProperties properties) {
        this.properties = properties;
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(properties.limitForPeriod())
            .limitRefreshPeriod(properties.limitRefreshPeriod())
            .timeoutDuration(properties.timeoutDuration())
            .build();
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter(properties.name());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor());
    }

    @Bean
    public HandlerInterceptor rateLimitingInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (rateLimiter.acquirePermission()) {
                    return true;
                }
                response.setStatus(429); // Too Many Requests
                return false;
            }
        };
    }

    @Bean
    public MeterBinder rateLimiterMetrics() {
        return registry -> {
            Gauge.builder("resilience4j.ratelimiter.available.permissions", rateLimiter, RateLimiter::getAvailablePermissions)
                .tag("name", properties.name())
                .register(registry);
            Gauge.builder("resilience4j.ratelimiter.waiting.threads", rateLimiter, rl -> rl.getMetrics().getNumberOfWaitingThreads())
                .tag("name", properties.name())
                .register(registry);
        };
    }
}
