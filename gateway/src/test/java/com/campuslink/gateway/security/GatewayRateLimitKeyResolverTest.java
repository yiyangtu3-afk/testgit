package com.campuslink.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GatewayRateLimitKeyResolverTest {

  private final GatewayRateLimitKeyResolver resolver = new GatewayRateLimitKeyResolver(
      "campuslink-test-signing-secret-must-have-32-bytes");

  @Test
  void usesAHashedAuthenticatedSubjectInsteadOfTheRawUserId() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/activities").build());
    exchange.getAttributes().put(GatewayJwtAuthenticationWebFilter.AUTHENTICATED_SUBJECT, "student-1");

    String key = resolver.resolve(exchange).block();

    assertThat(key).startsWith("user:").doesNotContain("student-1");
  }

  @Test
  void fallsBackToAHashedRemoteAddressForPublicRequests() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login")
        .remoteAddress(new InetSocketAddress("198.51.100.24", 50321)).build());

    String key = resolver.resolve(exchange).block();

    assertThat(key).startsWith("anonymous:").doesNotContain("198.51.100.24");
  }
}
