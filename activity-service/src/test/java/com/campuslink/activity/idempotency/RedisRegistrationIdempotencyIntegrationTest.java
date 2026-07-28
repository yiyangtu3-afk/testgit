package com.campuslink.activity.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisRegistrationIdempotencyIntegrationTest {
  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.4.2-alpine")).withExposedPorts(6379);

  @Test
  void replaysOneRegistrationResultFromRealRedis() {
    LettuceConnectionFactory connection = new LettuceConnectionFactory(
        redis.getHost(), redis.getMappedPort(6379));
    connection.afterPropertiesSet();
    try {
      StringRedisTemplate template = new StringRedisTemplate(connection);
      template.afterPropertiesSet();
      RedisRegistrationIdempotency idempotency = new RedisRegistrationIdempotency(template,
          new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry(),
          "campuslink-test-signing-secret-must-have-32-bytes", Duration.ofSeconds(30),
          Duration.ofHours(24));
      AtomicInteger actions = new AtomicInteger();
      RegistrationView expected = registration();

      assertThat(idempotency.execute("student-1", "activity-1", "request-key-0001", () -> {
        actions.incrementAndGet();
        return expected;
      })).isEqualTo(expected);
      assertThat(idempotency.execute("student-1", "activity-1", "request-key-0001", () -> {
        throw new AssertionError("a real Redis replay must not run the action");
      })).isEqualTo(expected);
      assertThat(actions).hasValue(1);
    } finally {
      connection.destroy();
    }
  }

  private static RegistrationView registration() {
    return new RegistrationView("registration-1", "activity-1", "registered", 0,
        LocalDateTime.of(2026, 8, 1, 9, 0), null);
  }
}
