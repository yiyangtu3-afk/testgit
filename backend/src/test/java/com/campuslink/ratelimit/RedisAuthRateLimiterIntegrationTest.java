package com.campuslink.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisAuthRateLimiterIntegrationTest {
  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.4.2-alpine")).withExposedPorts(6379);

  @Test
  void limitsVerificationCodeRequestsAndClearsSuccessfulLoginFailures() {
    LettuceConnectionFactory connection = new LettuceConnectionFactory(
        redis.getHost(), redis.getMappedPort(6379));
    connection.afterPropertiesSet();
    try {
      StringRedisTemplate template = new StringRedisTemplate(connection);
      template.afterPropertiesSet();
      RedisAuthRateLimiter limiter = new RedisAuthRateLimiter(template,
          new SimpleMeterRegistry(), "campuslink-test-signing-secret-must-have-32-bytes", 3,
          Duration.ofMinutes(1), 2, Duration.ofMinutes(5));

      limiter.acquireVerificationCode("13800000001");
      limiter.acquireVerificationCode("13800000001");
      limiter.acquireVerificationCode("13800000001");
      Throwable codeLimited = catchThrowable(
          () -> limiter.acquireVerificationCode("13800000001"));
      limiter.recordLoginFailure("13800000001");
      limiter.recordLoginFailure("13800000001");
      Throwable loginLimited = catchThrowable(
          () -> limiter.checkLoginAllowed("13800000001"));
      limiter.clearLoginFailures("13800000001");
      limiter.checkLoginAllowed("13800000001");

      assertThat(codeLimited).isInstanceOf(RateLimitExceededException.class);
      assertThat(loginLimited).isInstanceOf(RateLimitExceededException.class);
    } finally {
      connection.destroy();
    }
  }
}
