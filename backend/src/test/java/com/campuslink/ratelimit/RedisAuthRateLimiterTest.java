package com.campuslink.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisAuthRateLimiterTest {
  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  @Test
  void allowsConfiguredVerificationCodeRequestsThenReturnsTooManyRequests() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any())).thenReturn(1L, 2L, 3L, 4L);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisAuthRateLimiter limiter = limiter(redis, metrics, 3, Duration.ofMinutes(1), 5,
        Duration.ofMinutes(5));

    limiter.acquireVerificationCode("13800000001");
    limiter.acquireVerificationCode("13800000001");
    limiter.acquireVerificationCode("13800000001");
    Throwable error = catchThrowable(() -> limiter.acquireVerificationCode("13800000001"));

    assertThat(error).isInstanceOf(RateLimitExceededException.class)
        .hasMessage("验证码请求过于频繁，请稍后再试");
    assertThat(metrics.get("campuslink.redis.auth_rate_limit.allowed")
        .tag("action", "verification_code").counter().count()).isEqualTo(3);
    assertThat(metrics.get("campuslink.redis.auth_rate_limit.rejected")
        .tag("action", "verification_code").counter().count()).isEqualTo(1);
  }

  @Test
  void blocksLoginBeforeAnotherCredentialLookupAfterTheFailureLimit() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = org.mockito.Mockito.mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenReturn("5");
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisAuthRateLimiter limiter = limiter(redis, metrics, 3, Duration.ofMinutes(1), 5,
        Duration.ofMinutes(5));

    Throwable error = catchThrowable(() -> limiter.checkLoginAllowed("13800000001"));

    assertThat(error).isInstanceOf(RateLimitExceededException.class)
        .hasMessage("登录失败次数过多，请稍后再试");
    assertThat(metrics.get("campuslink.redis.auth_rate_limit.rejected")
        .tag("action", "login_failure").counter().count()).isEqualTo(1);
  }

  @Test
  void rejectsAuthenticationWhenRedisIsUnavailableAndRecordsTheError() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any()))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisAuthRateLimiter limiter = limiter(redis, metrics, 3, Duration.ofMinutes(1), 5,
        Duration.ofMinutes(5));

    Throwable error = catchThrowable(() -> limiter.acquireVerificationCode("13800000001"));

    assertThat(error).isInstanceOf(RateLimitUnavailableException.class)
        .hasMessage("认证限流服务暂不可用，请稍后重试");
    assertThat(metrics.get("campuslink.redis.auth_rate_limit.error")
        .tag("action", "verification_code").counter().count()).isEqualTo(1);
  }

  private RedisAuthRateLimiter limiter(
      StringRedisTemplate redis,
      SimpleMeterRegistry metrics,
      int codeLimit,
      Duration codeWindow,
      int loginLimit,
      Duration loginWindow) {
    return new RedisAuthRateLimiter(redis, metrics, SECRET, codeLimit, codeWindow, loginLimit,
        loginWindow);
  }
}
