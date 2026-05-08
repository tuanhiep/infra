package infra.brick.ratelimiter.api;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class TimeController {

    @GetMapping("/time")
    public String getTime() {
        return Instant.now().toString();
    }
}
