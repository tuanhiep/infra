package infra.brick.cdnedgecache.origin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assets")
class AssetController {

    private static final String APP_JS = """
            console.log("cdn-edge-cache demo: immutable asset served by the Origin");
            window.__CDN_EDGE_CACHE_DEMO__ = { version: "app.8f3a1c.js" };
            """;

    @GetMapping(value = "/app.8f3a1c.js", produces = "application/javascript")
    ResponseEntity<String> immutableAsset() {
        byte[] body = APP_JS.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/javascript"))
                .contentLength(body.length)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable())
                .eTag("\"app-8f3a1c\"")
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING)
                .body(APP_JS);
    }

    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> index() {
        String body = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <title>CDN Edge Cache Demo</title>
                    <script src="/assets/app.8f3a1c.js"></script>
                </head>
                <body>
                    <h1>CDN Edge Cache Demo</h1>
                </body>
                </html>
                """;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePublic()
                        .staleWhileRevalidate(java.time.Duration.ofMinutes(5)))
                .eTag("\"index-v1\"")
                .body(body);
    }
}
