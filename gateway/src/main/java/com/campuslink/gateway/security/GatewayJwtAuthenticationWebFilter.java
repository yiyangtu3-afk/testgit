package com.campuslink.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Enforces the public signed-JWT boundary before a route reaches the API service. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class GatewayJwtAuthenticationWebFilter implements WebFilter {

  private static final String GATEWAY_HEADER = "X-CampusLink-Gateway";
  private static final String GATEWAY_NAME = "campuslink-gateway";
  private final GatewayJwtTokenValidator jwtTokens;
  private final ObjectMapper objectMapper;

  public GatewayJwtAuthenticationWebFilter(
      GatewayJwtTokenValidator jwtTokens,
      ObjectMapper objectMapper) {
    this.jwtTokens = jwtTokens;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    exchange.getResponse().getHeaders().set(GATEWAY_HEADER, GATEWAY_NAME);
    if (!requiresAuthentication(exchange)) {
      return chain.filter(exchange);
    }
    try {
      jwtTokens.requireSubject(token(exchange));
      return chain.filter(exchange);
    } catch (SecurityException exception) {
      return writeUnauthorized(exchange, exception.getMessage());
    }
  }

  private boolean requiresAuthentication(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().value();
    if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
      return false;
    }
    if (path.startsWith("/ws/")) {
      return true;
    }
    return path.startsWith("/api/")
        && !path.startsWith("/api/auth/")
        && !"/api/database/health".equals(path);
  }

  private String token(ServerWebExchange exchange) {
    String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authorization != null && authorization.startsWith("Bearer ")) {
      String token = authorization.substring("Bearer ".length()).trim();
      if (!token.isBlank()) {
        return token;
      }
    }
    if (exchange.getRequest().getPath().value().startsWith("/ws/")) {
      String token = exchange.getRequest().getQueryParams().getFirst("token");
      if (token != null && !token.isBlank()) {
        return token;
      }
    }
    throw new SecurityException("请先登录");
  }

  private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
    try {
      byte[] body = objectMapper.writeValueAsBytes(Map.of("message", message));
      exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return exchange.getResponse().writeWith(Mono.just(
          exchange.getResponse().bufferFactory().wrap(body)));
    } catch (java.io.IOException exception) {
      return Mono.error(exception);
    }
  }
}
