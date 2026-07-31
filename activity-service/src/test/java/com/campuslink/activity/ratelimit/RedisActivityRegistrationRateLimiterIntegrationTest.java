package com.campuslink.activity.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class RedisActivityRegistrationRateLimiterIntegrationTest {
  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.4.2-alpine")).withExposedPorts(6379);

  @Test
  void appliesOneAtomicLimitToConcurrentCandidatesForTheSameActivity() throws Exception {
    LettuceConnectionFactory connection = new LettuceConnectionFactory(
        redis.getHost(), redis.getMappedPort(6379));
    connection.setShareNativeConnection(false);
    connection.afterPropertiesSet();
    try {
      StringRedisTemplate template = new StringRedisTemplate(connection);
      template.afterPropertiesSet();
      template.hasKey("registration-rate-limit-warmup");
      RedisActivityRegistrationRateLimiter limiter = new RedisActivityRegistrationRateLimiter(template,
          new SimpleMeterRegistry(), SECRET, 2, Duration.ofMinutes(1));

      ExecutorService executor = Executors.newFixedThreadPool(6);
      CountDownLatch ready = new CountDownLatch(6);
      CountDownLatch start = new CountDownLatch(1);
      try {
        List<Future<HttpStatus>> attempts = java.util.stream.IntStream.range(0, 6)
            .mapToObj(ignored -> executor.submit(() -> {
              ready.countDown();
              start.await(5, TimeUnit.SECONDS);
              try {
                limiter.acquireRegistration("activity-1");
                return HttpStatus.CREATED;
              } catch (ResponseStatusException exception) {
                return HttpStatus.valueOf(exception.getStatusCode().value());
              }
            }))
            .toList();
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<HttpStatus> statuses = attempts.stream()
            .map(attempt -> {
              try {
                return attempt.get(5, TimeUnit.SECONDS);
              } catch (Exception exception) {
                throw new AssertionError("并发报名限流测试未完成", exception);
              }
            })
            .toList();

        assertThat(statuses).filteredOn(HttpStatus.CREATED::equals).hasSize(2);
        assertThat(statuses).filteredOn(HttpStatus.TOO_MANY_REQUESTS::equals).hasSize(4);
      } finally {
        executor.shutdownNow();
      }
    } finally {
      connection.destroy();
    }
  }
}
