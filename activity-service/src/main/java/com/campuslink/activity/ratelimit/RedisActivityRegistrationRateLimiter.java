package com.campuslink.activity.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Applies a shared fixed-window admission limit before a hot activity reaches
 * the MySQL capacity transaction. MySQL row locking remains the authority for
 * capacity and waitlist order.
 */
@Component
@ConditionalOnProperty(
    name = "campuslink.redis.registration-rate-limit.enabled",
    havingValue = "true")
public class RedisActivityRegistrationRateLimiter implements ActivityRegistrationRateLimiter {
  private static final Logger log =
      LoggerFactory.getLogger(RedisActivityRegistrationRateLimiter.class);
  private static final String KEY_PREFIX = "campuslink:activities:registration-rate-limit:";
  private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
      "local count = redis.call('incr', KEYS[1]); "
          + "if count == 1 then redis.call('pexpire', KEYS[1], ARGV[1]); "
          + "end; return count;",
      Long.class);

  private final StringRedisTemplate redis;
  private final byte[] signingSecret;
  private final Duration window;
  private final int maximum;
  private final Counter allowed;
  private final Counter rejected;
  private final Counter errors;

  public RedisActivityRegistrationRateLimiter(
      StringRedisTemplate redis,
      MeterRegistry metrics,
      @Value("${campuslink.security.jwt.secret}") String signingSecret,
      @Value("${campuslink.redis.registration-rate-limit.activity-limit:60}") int maximum,
      @Value("${campuslink.redis.registration-rate-limit.window:1m}") Duration window) {
    if (maximum < 1 || window.isZero() || window.isNegative() || window.toMillis() < 1) {
      throw new IllegalArgumentException("Redis 活动报名限流配置必须为正数");
    }
    this.redis = redis;
    this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    this.maximum = maximum;
    this.window = window;
    this.allowed = counter(metrics, "campuslink.redis.activity_registration_rate_limit.allowed");
    this.rejected = counter(metrics, "campuslink.redis.activity_registration_rate_limit.rejected");
    this.errors = counter(metrics, "campuslink.redis.activity_registration_rate_limit.error");
  }

  @Override
  public void acquireRegistration(String activityId) {
    try {
      Long count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key(activityId)),
          Long.toString(window.toMillis()));
      if (count == null) {
        unavailable("Redis 未返回限流计数");
        return;
      }
      if (count > maximum) {
        rejected.increment();
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            "当前活动报名人数较多，请稍后再试");
      }
      allowed.increment();
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      unavailable(exception.toString());
    }
  }

  private void unavailable(String reason) {
    errors.increment();
    log.warn("Redis 活动报名限流不可用，回退 MySQL 事务与行锁: {}", reason);
  }

  private String key(String activityId) {
    return KEY_PREFIX + fingerprint("registration\u001f" + activityId);
  }

  private String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("无法生成活动报名限流键", exception);
    }
  }

  private Counter counter(MeterRegistry metrics, String name) {
    return Counter.builder(name).register(metrics);
  }
}
