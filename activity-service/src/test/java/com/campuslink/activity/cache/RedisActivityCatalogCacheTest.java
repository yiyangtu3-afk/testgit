package com.campuslink.activity.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class RedisActivityCatalogCacheTest {

  @Test
  void cachesThePublicCatalogByNormalizedFilterKey() {
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    var registry = new SimpleMeterRegistry();
    var cache = cache(redis, registry);
    var databaseCalls = new AtomicInteger();
    List<ActivityView> expected = List.of(activity());

    assertThat(cache.load("技术", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), () -> {
      databaseCalls.incrementAndGet();
      return expected;
    })).isEqualTo(expected);

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
    verify(values).set(key.capture(), payload.capture(), any(Duration.class));
    when(values.get(key.getValue())).thenReturn(payload.getValue());

    assertThat(cache.load("技术", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), () -> {
      throw new AssertionError("a cache hit must not load MySQL");
    })).isEqualTo(expected);
    assertThat(databaseCalls).hasValue(1);
    assertThat(key.getValue()).contains("campuslink:activities:catalog:v0:");
    assertThat(registry.get("campuslink.redis.activity_catalog.cache.miss").counter().count())
        .isEqualTo(1);
    assertThat(registry.get("campuslink.redis.activity_catalog.cache.hit").counter().count())
        .isEqualTo(1);
  }

  @Test
  void fallsBackToMySqlWhenRedisIsUnavailable() {
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("Redis unavailable"));
    var registry = new SimpleMeterRegistry();
    var cache = cache(redis, registry);
    List<ActivityView> expected = List.of(activity());

    assertThat(cache.load(null, null, null, () -> expected)).isEqualTo(expected);
    assertThat(registry.get("campuslink.redis.activity_catalog.cache.error").counter().count())
        .isEqualTo(1);
  }

  @Test
  void invalidationAdvancesTheCatalogVersionInsteadOfScanningKeys() {
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);

    cache(redis).invalidate();

    verify(values).increment("campuslink:activities:catalog:version");
  }

  @Test
  void waitsForTheLockOwnerToPopulateTheCatalogInsteadOfLoadingMySqlAgain() throws Exception {
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    String payload = new ObjectMapper().findAndRegisterModules().writeValueAsString(List.of(activity()));
    AtomicInteger catalogReads = new AtomicInteger();
    when(values.get(anyString())).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      if ("campuslink:activities:catalog:version".equals(key)) {
        return null;
      }
      return catalogReads.incrementAndGet() == 1 ? null : payload;
    });
    var registry = new SimpleMeterRegistry();
    var cache = cache(redis, registry, Duration.ZERO, 1);
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

    assertThat(cache.load(null, null, null, () -> {
      throw new AssertionError("a waiting request must not load MySQL");
    })).containsExactly(activity());

    verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    assertThat(registry.get("campuslink.redis.activity_catalog.cache.lock.wait_hit").counter().count())
        .isEqualTo(1);
  }

  private RedisActivityCatalogCache cache(StringRedisTemplate redis) {
    return cache(redis, new SimpleMeterRegistry());
  }

  private RedisActivityCatalogCache cache(StringRedisTemplate redis, SimpleMeterRegistry registry) {
    return cache(redis, registry, Duration.ofMillis(25), 4);
  }

  private RedisActivityCatalogCache cache(
      StringRedisTemplate redis, SimpleMeterRegistry registry, Duration retryDelay, int retries) {
    ValueOperations<String, String> values = redis.opsForValue();
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    return new RedisActivityCatalogCache(redis, new ObjectMapper().findAndRegisterModules(),
        registry, Duration.ofMinutes(2), Duration.ZERO, Duration.ofSeconds(5), retryDelay, retries);
  }

  private ActivityView activity() {
    return new ActivityView("activity-1", "校园编程赛", "描述", "技术", "创新中心",
        LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 11, 0), 30,
        "teacher-1", "李老师", "published", "approved", null, "admin-1", "管理员",
        LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 6, 1, 9, 0));
  }
}
