package com.campuslink.activity.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RedisActivityRegistrationRateLimiterTest {
  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  @Test
  void rejectsRequestsAfterTheSharedActivityLimit() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any())).thenReturn(1L, 2L, 3L);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisActivityRegistrationRateLimiter limiter = new RedisActivityRegistrationRateLimiter(redis,
        metrics, SECRET, 2, Duration.ofMinutes(1));

    limiter.acquireRegistration("activity-1");
    limiter.acquireRegistration("activity-1");
    Throwable error = catchThrowable(() -> limiter.acquireRegistration("activity-1"));

    assertThat(error).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(metrics.get("campuslink.redis.activity_registration_rate_limit.allowed")
        .counter().count()).isEqualTo(2);
    assertThat(metrics.get("campuslink.redis.activity_registration_rate_limit.rejected")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void fallsBackToTheMysqlCapacityTransactionWhenRedisIsUnavailable() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any()))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisActivityRegistrationRateLimiter limiter = new RedisActivityRegistrationRateLimiter(redis,
        metrics, SECRET, 2, Duration.ofMinutes(1));

    limiter.acquireRegistration("activity-1");

    assertThat(metrics.get("campuslink.redis.activity_registration_rate_limit.error")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void fingerprintsTheActivityIdBeforeUsingItAsARedisKey() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any())).thenAnswer(invocation -> {
      List<String> keys = invocation.getArgument(1);
      assertThat(keys).singleElement().satisfies(key -> assertThat(key)
          .startsWith("campuslink:activities:registration-rate-limit:")
          .doesNotContain("activity-1"));
      return 1L;
    });
    RedisActivityRegistrationRateLimiter limiter = new RedisActivityRegistrationRateLimiter(redis,
        new SimpleMeterRegistry(), SECRET, 2, Duration.ofMinutes(1));

    limiter.acquireRegistration("activity-1");
  }

  @Test
  void rejectsAWindowTooShortForRedisMillisecondExpiry() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);

    assertThatIllegalArgumentException().isThrownBy(() ->
        new RedisActivityRegistrationRateLimiter(redis, new SimpleMeterRegistry(), SECRET, 1,
            Duration.ofNanos(1)));
  }
}
