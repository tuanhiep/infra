package infra.brick.cdnedgecache.origin;

import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ProductController {

    @GetMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductView> product(@PathVariable String id) {
        ProductView body = new ProductView(
                id,
                "Kettlebell 35 lb",
                5900,
                "USD",
                Instant.now().toString());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(60)).cachePublic()
                        .staleWhileRevalidate(java.time.Duration.ofMinutes(10)))
                .eTag("\"product-" + id + "-v1\"")
                .body(body);
    }

    @PostMapping(value = "/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CheckoutResult> checkout() {
        CheckoutResult body = new CheckoutResult("accepted", Instant.now().toString());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    record ProductView(String id, String name, int priceCents, String currency, String generatedAt) {}

    record CheckoutResult(String status, String acceptedAt) {}
}
