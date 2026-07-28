package com.campuslink.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class GatewayJwtAuthenticationWebFilterTest {

  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  private final GatewayJwtAuthenticationWebFilter filter = new GatewayJwtAuthenticationWebFilter(
      new GatewayJwtTokenValidator(
          SECRET,
          Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC)),
      new ObjectMapper());

  @Test
  void rejectsUnauthenticatedApiBeforeItCanReachDownstream() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/activities").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void allowsPublicHealthAndLoginRoutes() {
    assertEquals("continued", responseFor("/api/database/health"));
    assertEquals("continued", responseFor("/api/auth/login"));
  }

  @Test
  void identifiesGatewayResponsesWithoutChangingTheBodyContract() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/database/health").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals("campuslink-gateway",
        exchange.getResponse().getHeaders().getFirst("X-CampusLink-Gateway"));
  }

  @Test
  void storesTheVerifiedSubjectForTheRouteRateLimitKeyResolver() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/activities")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("student-1", 1_785_008_000L)).build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals("student-1",
        exchange.getAttribute(GatewayJwtAuthenticationWebFilter.AUTHENTICATED_SUBJECT));
  }

  private String responseFor(String path) {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    AtomicBoolean continued = new AtomicBoolean();
    filter.filter(exchange, ignored -> {
      continued.set(true);
      return Mono.empty();
    }).block();
    return continued.get() ? "continued" : "rejected";
  }

  private static String token(String subject, long expiration) {
    String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    String payload = encode("{\"sub\":\"" + subject + "\",\"exp\":" + expiration + "}");
    String signed = header + "." + payload;
    return signed + "." + sign(signed);
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(
          mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
