package infra.brick.cdnedgecache.origin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class OriginCacheHeaderIntegrationTest {

    @Test
    void immutableAssetHasLongLivedCacheHeaders() {
        ResponseEntity<String> response = new AssetController().immutableAsset();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=31536000")
                .contains("public")
                .contains("immutable");
        assertThat(response.getHeaders().getETag()).isEqualTo("\"app-8f3a1c\"");
        assertThat(response.getHeaders().getFirst(HttpHeaders.VARY)).isEqualTo(HttpHeaders.ACCEPT_ENCODING);
    }

    @Test
    void cacheableApiUsesShortTtlAndStaleWhileRevalidate() {
        ResponseEntity<ProductController.ProductView> response = new ProductController().product("42");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=60")
                .contains("public")
                .contains("stale-while-revalidate=600");
        assertThat(response.getHeaders().getETag()).isEqualTo("\"product-42-v1\"");
    }

    @Test
    void stateChangingApiIsNotStoredBySharedCaches() {
        ResponseEntity<ProductController.CheckoutResult> response = new ProductController().checkout();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }
}
