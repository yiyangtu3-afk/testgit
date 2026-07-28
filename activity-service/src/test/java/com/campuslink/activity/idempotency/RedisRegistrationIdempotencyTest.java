package com.campuslink.activity.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SuppressWarnings("unchecked")
class RedisRegistrationIdempotencyTest {
  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  @Test
  void replaysTheCompletedRegistrationWithoutRunningTheActionAgain() throws Exception {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    RegistrationView expected = registration();
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
    when(values.get(anyString())).thenReturn("completed:" + json.writeValueAsString(expected));
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisRegistrationIdempotency idempotency = idempotency(redis, json, metrics);

    assertThat(idempotency.execute("student-1", "activity-1", "request-key-0001", () -> {
      throw new AssertionError("a completed request must not run again");
    })).isEqualTo(expected);
    assertThat(metrics.get("campuslink.redis.registration_idempotency.replayed").counter().count())
        .isEqualTo(1);
  }

  @Test
  void reportsARealConflictWhileTheFirstRequestIsStillRunning() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
    when(values.get(anyString())).thenReturn("processing:another-request");

    Throwable error = catchThrowable(() -> idempotency(redis).execute(
        "student-1", "activity-1", "request-key-0001", RedisRegistrationIdempotencyTest::registration));

    assertThat(error).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void fallsBackToTheExistingRegistrationActionWhenRedisIsUnavailable() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

    assertThat(idempotency(redis).execute("student-1", "activity-1", "request-key-0001",
        RedisRegistrationIdempotencyTest::registration)).isEqualTo(registration());
  }

  private RedisRegistrationIdempotency idempotency(StringRedisTemplate redis) {
    return idempotency(redis, new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry());
  }

  private RedisRegistrationIdempotency idempotency(
      StringRedisTemplate redis, ObjectMapper json, SimpleMeterRegistry metrics) {
    return new RedisRegistrationIdempotency(redis, json, metrics, SECRET,
        Duration.ofSeconds(30), Duration.ofHours(24));
  }

  private static RegistrationView registration() {
    return new RegistrationView("registration-1", "activity-1", "registered", 0,
        LocalDateTime.of(2026, 8, 1, 9, 0), null);
  }
}
