package com.tuanhiep;

import com.tuanhiep.config.CircuitBreakerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CircuitBreakerProperties.class)
public class CircuitBreakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CircuitBreakerApplication.class, args);
    }
}
