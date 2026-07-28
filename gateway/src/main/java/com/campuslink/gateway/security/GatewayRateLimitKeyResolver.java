package com.campuslink.gateway.security;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Creates privacy-preserving Redis rate-limit keys at the public Gateway boundary. */
@Component("gatewayRateLimitKeyResolver")
@ConditionalOnProperty(name = "campuslink.redis.gateway-rate-limit.enabled", havingValue = "true")
public class GatewayRateLimitKeyResolver implements KeyResolver {

  private final byte[] signingSecret;

  public GatewayRateLimitKeyResolver(@Value("${campuslink.security.jwt.secret}") String signingSecret) {
    this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    Object subject = exchange.getAttribute(GatewayJwtAuthenticationWebFilter.AUTHENTICATED_SUBJECT);
    if (subject instanceof String value && !value.isBlank()) {
      return Mono.just("user:" + fingerprint(value));
    }
    return Mono.just("anonymous:" + fingerprint(remoteAddress(exchange)));
  }

  private String remoteAddress(ServerWebExchange exchange) {
    InetSocketAddress address = exchange.getRequest().getRemoteAddress();
    if (address == null || address.getAddress() == null) {
      return "unknown";
    }
    return address.getAddress().getHostAddress();
  }

  private String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("无法生成网关限流键", exception);
    }
  }
}
