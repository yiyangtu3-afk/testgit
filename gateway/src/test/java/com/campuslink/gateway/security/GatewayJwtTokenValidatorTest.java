package com.campuslink.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class GatewayJwtTokenValidatorTest {

  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void acceptsCurrentSignedToken() {
    GatewayJwtTokenValidator validator = new GatewayJwtTokenValidator(SECRET, CLOCK);

    assertEquals("u-1001", validator.requireSubject(token("u-1001", 1_785_008_000L)));
  }

  @Test
  void rejectsExpiredAndTamperedTokens() {
    GatewayJwtTokenValidator validator = new GatewayJwtTokenValidator(SECRET, CLOCK);

    assertThrows(SecurityException.class, () -> validator.requireSubject(token("u-1001", 1_753_388_799L)));
    assertThrows(SecurityException.class, () -> validator.requireSubject(token("u-1001", 1_785_008_000L) + "x"));
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
