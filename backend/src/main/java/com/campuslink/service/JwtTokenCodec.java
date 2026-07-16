package com.campuslink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenCodec {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final ObjectMapper JSON = new ObjectMapper();
  private final byte[] secret;
  private final long ttlSeconds;
  private final Clock clock;

  @Autowired
  public JwtTokenCodec(
      @Value("${campuslink.security.jwt.secret}") String secret,
      @Value("${campuslink.security.jwt.ttl-seconds}") long ttlSeconds) {
    this(secret, ttlSeconds, Clock.systemUTC());
  }

  JwtTokenCodec(String secret, long ttlSeconds, Clock clock) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("JWT 签名密钥至少需要 32 字节");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.ttlSeconds = ttlSeconds;
    this.clock = clock;
  }

  public String issue(String userId) {
    long issuedAt = clock.instant().getEpochSecond();
    String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    String payload = encode("{\"sub\":\"" + userId + "\",\"iat\":" + issuedAt
        + ",\"exp\":" + (issuedAt + ttlSeconds) + "}");
    String signed = header + "." + payload;
    return signed + "." + sign(signed);
  }

  public String requireSubject(String token) {
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3 || !MessageDigest.isEqual(
        sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII),
        parts[2].getBytes(StandardCharsets.US_ASCII))) {
      throw new SecurityException("登录令牌无效");
    }
    try {
      JsonNode claims = JSON.readTree(DECODER.decode(parts[1]));
      if (claims.path("exp").asLong() <= clock.instant().getEpochSecond()
          || claims.path("sub").asText().isBlank()) {
        throw new SecurityException("登录已过期，请重新登录");
      }
      return claims.path("sub").asText();
    } catch (IllegalArgumentException | java.io.IOException exception) {
      throw new SecurityException("登录令牌无效");
    }
  }

  private String encode(String value) {
    return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (Exception exception) {
      throw new IllegalStateException("无法验证登录令牌", exception);
    }
  }
}
