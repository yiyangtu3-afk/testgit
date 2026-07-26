package com.campuslink.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class GatewayJwtAuthenticationWebFilterTest {

  private final GatewayJwtAuthenticationWebFilter filter = new GatewayJwtAuthenticationWebFilter(
      new GatewayJwtTokenValidator(
          "campuslink-test-signing-secret-must-have-32-bytes",
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

  private String responseFor(String path) {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    AtomicBoolean continued = new AtomicBoolean();
    filter.filter(exchange, ignored -> {
      continued.set(true);
      return Mono.empty();
    }).block();
    return continued.get() ? "continued" : "rejected";
  }
}
