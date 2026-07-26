package com.campuslink.activity.service;

import com.campuslink.activity.domain.UserDirectoryEntry;
import com.campuslink.activity.mapper.AuthSessionMapper;
import com.campuslink.activity.mapper.UserDirectoryMapper;
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

/** Rechecks the signed, non-revoked bearer token; gateway headers are never trusted. */
@Service
public class ActivityAuthService {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final AuthSessionMapper sessions;
  private final UserDirectoryMapper users;
  private final byte[] secret;
  private final Clock clock;

  @Autowired
  public ActivityAuthService(AuthSessionMapper sessions, UserDirectoryMapper users,
      @Value("${campuslink.security.jwt.secret}") String secret) {
    this(sessions, users, secret, Clock.systemUTC());
  }

  ActivityAuthService(AuthSessionMapper sessions, UserDirectoryMapper users, String secret, Clock clock) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT 签名密钥至少需要 32 字节");
    this.sessions = sessions; this.users = users; this.secret = secret.getBytes(StandardCharsets.UTF_8); this.clock = clock;
  }

  public UserDirectoryEntry requireUser(String authorization) {
    String token = bearer(authorization);
    String subject = subject(token);
    if (!subject.equals(sessions.userId(token))) throw unauthorized("登录已失效，请重新登录");
    UserDirectoryEntry user = users.find(subject);
    if (user == null) throw unauthorized("登录已失效，请重新登录");
    return user;
  }

  private String bearer(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) throw unauthorized("请先登录");
    String token = authorization.substring(7).trim();
    if (token.isBlank()) throw unauthorized("请先登录");
    return token;
  }
  private String subject(String token) {
    String[] p = token.split("\\.", -1);
    if (p.length != 3 || !MessageDigest.isEqual(sign(p[0] + "." + p[1]).getBytes(StandardCharsets.US_ASCII), p[2].getBytes(StandardCharsets.US_ASCII))) throw unauthorized("登录令牌无效");
    try {
      JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(p[1]));
      String sub = claims.path("sub").asText();
      if (sub.isBlank() || claims.path("exp").asLong() <= clock.instant().getEpochSecond()) throw unauthorized("登录已过期，请重新登录");
      return sub;
    } catch (IllegalArgumentException | java.io.IOException exception) { throw unauthorized("登录令牌无效"); }
  }
  private String sign(String value) {
    try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.US_ASCII))); }
    catch (Exception exception) { throw new IllegalStateException("无法验证登录令牌", exception); }
  }
  private ResponseStatusException unauthorized(String message) { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message); }
}
