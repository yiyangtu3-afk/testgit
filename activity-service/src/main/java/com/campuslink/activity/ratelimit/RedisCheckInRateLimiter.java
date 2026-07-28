package com.campuslink.activity.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "campuslink.redis.check-in-rate-limit.enabled", havingValue = "true")
public class RedisCheckInRateLimiter implements CheckInRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisCheckInRateLimiter.class);
  private static final String KEY_PREFIX = "campuslink:activities:check-in-rate-limit:";
  private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
      "local count = redis.call('incr', KEYS[1]); "
          + "if count == 1 then redis.call('pexpire', KEYS[1], ARGV[1]); end; "
          + "return count;",
      Long.class);

  private final StringRedisTemplate redis;
  private final Duration window;
  private final Limit credentialIssue;
  private final Limit credentialVerification;

  public RedisCheckInRateLimiter(
      StringRedisTemplate redis,
      MeterRegistry metrics,
      @Value("${campuslink.redis.check-in-rate-limit.credential-limit:5}") int credentialLimit,
      @Value("${campuslink.redis.check-in-rate-limit.verification-limit:10}") int verificationLimit,
      @Value("${campuslink.redis.check-in-rate-limit.window:1m}") Duration window) {
    if (credentialLimit < 1 || verificationLimit < 1 || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("Redis 签到限流配置必须为正数");
    }
    this.redis = redis;
    this.window = window;
    this.credentialIssue = limit(metrics, "credential_issue", credentialLimit);
    this.credentialVerification = limit(metrics, "credential_verification", verificationLimit);
  }

  @Override
  public void acquireCredentialIssue(String userId, String activityId) {
    acquire(credentialIssue, userId, activityId);
  }

  @Override
  public void acquireCredentialVerification(String userId, String activityId) {
    acquire(credentialVerification, userId, activityId);
  }

  private void acquire(Limit limit, String userId, String activityId) {
    try {
      Long count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key(limit.action(), userId, activityId)),
          Long.toString(window.toMillis()));
      if (count == null) {
        unavailable(limit, "Redis 未返回限流计数");
        return;
      }
      if (count > limit.maximum()) {
        limit.rejected().increment();
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
      }
      limit.allowed().increment();
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      unavailable(limit, exception.toString());
    }
  }

  private void unavailable(Limit limit, String reason) {
    limit.errors().increment();
    log.warn("Redis 签到限流不可用，按可用性策略放行 action={}: {}", limit.action(), reason);
  }

  private Limit limit(MeterRegistry metrics, String action, int maximum) {
    return new Limit(action, maximum,
        counter(metrics, "campuslink.redis.check_in_rate_limit.allowed", action),
        counter(metrics, "campuslink.redis.check_in_rate_limit.rejected", action),
        counter(metrics, "campuslink.redis.check_in_rate_limit.error", action));
  }

  private Counter counter(MeterRegistry metrics, String name, String action) {
    return Counter.builder(name).tag("action", action).register(metrics);
  }

  private String key(String action, String userId, String activityId) {
    String value = action + "\u001f" + userId + "\u001f" + activityId;
    return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private record Limit(String action, int maximum, Counter allowed, Counter rejected, Counter errors) {}
}
