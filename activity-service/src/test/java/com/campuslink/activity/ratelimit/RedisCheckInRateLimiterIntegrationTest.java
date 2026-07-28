package com.campuslink.activity.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisCheckInRateLimiterIntegrationTest {
  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.4.2-alpine")).withExposedPorts(6379);

  @Test
  void limitsEachUserActivityAndActionWithRealRedisCounters() {
    LettuceConnectionFactory connection = new LettuceConnectionFactory(
        redis.getHost(), redis.getMappedPort(6379));
    connection.afterPropertiesSet();
    try {
      StringRedisTemplate template = new StringRedisTemplate(connection);
      template.afterPropertiesSet();
      RedisCheckInRateLimiter limiter = new RedisCheckInRateLimiter(template,
          new SimpleMeterRegistry(), 2, 2, Duration.ofMinutes(1));

      limiter.acquireCredentialIssue("student-1", "activity-1");
      limiter.acquireCredentialIssue("student-1", "activity-1");
      Throwable limited = catchThrowable(
          () -> limiter.acquireCredentialIssue("student-1", "activity-1"));
      limiter.acquireCredentialIssue("student-1", "activity-2");
      limiter.acquireCredentialVerification("student-1", "activity-1");

      assertThat(limited).isInstanceOf(ResponseStatusException.class);
      assertThat(((ResponseStatusException) limited).getStatusCode())
          .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    } finally {
      connection.destroy();
    }
  }
}
