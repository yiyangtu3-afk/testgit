package com.campuslink.gateway.resilience;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Returns a real, stable API failure when an isolated downstream circuit is open. */
@RestController
public class GatewayFallbackController {

  @RequestMapping("/_gateway/fallback/{service}")
  Mono<ResponseEntity<Map<String, String>>> unavailable(@PathVariable String service) {
    String message = switch (service) {
      case "activity" -> "活动服务暂时不可用，请稍后重试";
      case "notification" -> "通知服务暂时不可用，请稍后重试";
      default -> "下游服务暂时不可用，请稍后重试";
    };
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("message", message)));
  }
}
