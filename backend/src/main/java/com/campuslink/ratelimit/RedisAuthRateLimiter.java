package com.campuslink.ratelimit;

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
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campuslink.redis.auth-rate-limit.enabled", havingValue = "true")
public class RedisAuthRateLimiter implements AuthRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisAuthRateLimiter.class);
  private static final String PREFIX = "campuslink:auth:rate-limit:";
  private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
      "local count = redis.call('incr', KEYS[1]); "
          + "if count == 1 then redis.call('pexpire', KEYS[1], ARGV[1]); end; "
          + "return count;",
      Long.class);

  private final StringRedisTemplate redis;
  private final byte[] signingSecret;
  private final Limit verificationCode;
  private final Limit loginFailure;

  public RedisAuthRateLimiter(
      StringRedisTemplate redis,
      MeterRegistry metrics,
      @Value("${campuslink.security.jwt.secret}") String signingSecret,
      @Value("${campuslink.redis.auth-rate-limit.verification-code-limit:3}") int verificationCodeLimit,
      @Value("${campuslink.redis.auth-rate-limit.verification-code-window:1m}") Duration verificationCodeWindow,
      @Value("${campuslink.redis.auth-rate-limit.login-failure-limit:5}") int loginFailureLimit,
      @Value("${campuslink.redis.auth-rate-limit.login-failure-window:5m}") Duration loginFailureWindow) {
    if (verificationCodeLimit < 1 || loginFailureLimit < 1
        || verificationCodeWindow.isZero() || verificationCodeWindow.isNegative()
        || loginFailureWindow.isZero() || loginFailureWindow.isNegative()) {
      throw new IllegalArgumentException("Redis 认证限流配置必须为正数");
    }
    this.redis = redis;
    this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    this.verificationCode = limit(metrics, "verification_code", verificationCodeLimit,
        verificationCodeWindow);
    this.loginFailure = limit(metrics, "login_failure", loginFailureLimit, loginFailureWindow);
  }

  @Override
  public void acquireVerificationCode(String phone) {
    incrementAndRejectAtLimit(verificationCode, phone, "验证码请求过于频繁，请稍后再试");
  }

  @Override
  public void checkLoginAllowed(String phone) {
    try {
      String count = redis.opsForValue().get(key(loginFailure, phone));
      if (count != null && Long.parseLong(count) >= loginFailure.maximum()) {
        loginFailure.rejected().increment();
        throw new RateLimitExceededException("登录失败次数过多，请稍后再试");
      }
    } catch (RateLimitExceededException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      unavailable(loginFailure, exception);
    }
  }

  @Override
  public void recordLoginFailure(String phone) {
    incrementAndRejectAtLimit(loginFailure, phone, "登录失败次数过多，请稍后再试");
  }

  @Override
  public void clearLoginFailures(String phone) {
    try {
      redis.delete(key(loginFailure, phone));
    } catch (RuntimeException exception) {
      unavailable(loginFailure, exception);
    }
  }

  private void incrementAndRejectAtLimit(Limit limit, String phone, String message) {
    try {
      Long count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key(limit, phone)),
          Long.toString(limit.window().toMillis()));
      if (count == null) {
        throw new IllegalStateException("Redis 未返回限流计数");
      }
      if (count > limit.maximum()) {
        limit.rejected().increment();
        throw new RateLimitExceededException(message);
      }
      limit.allowed().increment();
    } catch (RateLimitExceededException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      unavailable(limit, exception);
    }
  }

  private Limit limit(MeterRegistry metrics, String action, int maximum, Duration window) {
    return new Limit(action, maximum, window,
        counter(metrics, "campuslink.redis.auth_rate_limit.allowed", action),
        counter(metrics, "campuslink.redis.auth_rate_limit.rejected", action),
        counter(metrics, "campuslink.redis.auth_rate_limit.error", action));
  }

  private Counter counter(MeterRegistry metrics, String name, String action) {
    return Counter.builder(name).tag("action", action).register(metrics);
  }

  private String key(Limit limit, String phone) {
    return PREFIX + limit.action() + ":" + fingerprint(phone);
  }

  private String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("无法生成认证限流键", exception);
    }
  }

  private void unavailable(Limit limit, RuntimeException exception) {
    limit.errors().increment();
    log.warn("Redis 认证限流不可用，拒绝 action={}: {}", limit.action(), exception.toString());
    throw new RateLimitUnavailableException("认证限流服务暂不可用，请稍后重试", exception);
  }

  private record Limit(
      String action,
      int maximum,
      Duration window,
      Counter allowed,
      Counter rejected,
      Counter errors) {
  }
}
