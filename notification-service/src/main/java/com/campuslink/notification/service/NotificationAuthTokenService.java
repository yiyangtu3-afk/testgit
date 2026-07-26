package com.campuslink.notification.service;

import com.campuslink.notification.mapper.AuthSessionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Revalidates signature, expiry, and persisted session after the gateway boundary. */
@Service
public class NotificationAuthTokenService {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final AuthSessionMapper sessions;
  private final byte[] secret;
  private final Clock clock;

  @Autowired
  public NotificationAuthTokenService(AuthSessionMapper sessions,
      @Value("${campuslink.security.jwt.secret}") String secret) {
    this(sessions, secret, Clock.systemUTC());
  }

  NotificationAuthTokenService(AuthSessionMapper sessions, String secret, Clock clock) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("JWT 签名密钥至少需要 32 字节");
    }
    this.sessions = sessions;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
  }

  public String requireUserId(String authorization) {
    String token = bearerToken(authorization);
    String subject = requireSubject(token);
    String userId = sessions.findUserIdByToken(token);
    if (userId == null || !subject.equals(userId)) {
      throw unauthorized("登录已失效，请重新登录");
    }
    return userId;
  }

  private String bearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw unauthorized("请先登录");
    }
    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isBlank()) throw unauthorized("请先登录");
    return token;
  }

  private String requireSubject(String token) {
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1])
        .getBytes(StandardCharsets.US_ASCII), parts[2].getBytes(StandardCharsets.US_ASCII))) {
      throw unauthorized("登录令牌无效");
    }
    try {
      JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
      String subject = claims.path("sub").asText();
      if (claims.path("exp").asLong() <= clock.instant().getEpochSecond() || subject.isBlank()) {
        throw unauthorized("登录已过期，请重新登录");
      }
      return subject;
    } catch (IllegalArgumentException | java.io.IOException error) {
      throw unauthorized("登录令牌无效");
    }
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(
          mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (Exception error) {
      throw new IllegalStateException("无法验证登录令牌", error);
    }
  }

  private ResponseStatusException unauthorized(String message) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
  }
}
