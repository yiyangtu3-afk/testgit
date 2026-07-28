package com.campuslink.activity.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisActivityCatalogCacheIntegrationTest {
  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.4.2-alpine")).withExposedPorts(6379);

  @Test
  void storesAndReadsThePublicCatalogFromARealRedisServer() {
    LettuceConnectionFactory connection = new LettuceConnectionFactory(
        redis.getHost(), redis.getMappedPort(6379));
    connection.afterPropertiesSet();
    try {
      StringRedisTemplate template = new StringRedisTemplate(connection);
      template.afterPropertiesSet();
      RedisActivityCatalogCache cache = new RedisActivityCatalogCache(template,
          new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry(),
          Duration.ofMinutes(2), Duration.ZERO);
      AtomicInteger databaseCalls = new AtomicInteger();
      List<ActivityView> expected = List.of(activity());

      assertThat(cache.load("技术", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), () -> {
        databaseCalls.incrementAndGet();
        return expected;
      })).isEqualTo(expected);
      assertThat(cache.load("技术", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), () -> {
        throw new AssertionError("a Redis cache hit must not query MySQL");
      })).isEqualTo(expected);
      assertThat(databaseCalls).hasValue(1);
    } finally {
      connection.destroy();
    }
  }

  private ActivityView activity() {
    return new ActivityView("activity-1", "校园编程赛", "描述", "技术", "创新中心",
        LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 11, 0), 30,
        "teacher-1", "李老师", "published", "approved", null, "admin-1", "管理员",
        LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 6, 1, 9, 0));
  }
}
