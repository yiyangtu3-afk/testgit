package com.campuslink.activity.cache;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campuslink.redis.activity-catalog.enabled", havingValue = "true")
public class RedisActivityCatalogCache implements ActivityCatalogCache {
  private static final Logger log = LoggerFactory.getLogger(RedisActivityCatalogCache.class);
  private static final String VERSION_KEY = "campuslink:activities:catalog:version";
  private static final String KEY_PREFIX = "campuslink:activities:catalog:v";

  private final StringRedisTemplate redis;
  private final ObjectMapper json;
  private final JavaType listType;
  private final Duration ttl;
  private final Duration jitter;
  private final Counter hits;
  private final Counter misses;
  private final Counter errors;
  private final Counter invalidations;

  public RedisActivityCatalogCache(
      StringRedisTemplate redis,
      ObjectMapper json,
      MeterRegistry metrics,
      @Value("${campuslink.redis.activity-catalog.ttl:2m}") Duration ttl,
      @Value("${campuslink.redis.activity-catalog.ttl-jitter:15s}") Duration jitter) {
    this.redis = redis;
    this.json = json;
    this.listType = json.getTypeFactory().constructCollectionType(List.class, ActivityView.class);
    this.ttl = ttl;
    this.jitter = jitter;
    this.hits = counter(metrics, "campuslink.redis.activity_catalog.cache.hit");
    this.misses = counter(metrics, "campuslink.redis.activity_catalog.cache.miss");
    this.errors = counter(metrics, "campuslink.redis.activity_catalog.cache.error");
    this.invalidations = counter(metrics, "campuslink.redis.activity_catalog.cache.invalidate");
  }

  @Override
  public List<ActivityView> load(
      String category, LocalDate from, LocalDate to, Supplier<List<ActivityView>> databaseLoader) {
    String key;
    String cached;
    try {
      key = cacheKey(category, from, to, currentVersion());
      cached = redis.opsForValue().get(key);
    } catch (RuntimeException exception) {
      error("读取活动目录缓存失败，已回源 MySQL", exception);
      return databaseLoader.get();
    }
    if (cached != null) {
      try {
        List<ActivityView> value = json.readValue(cached, listType);
        hits.increment();
        return value;
      } catch (JsonProcessingException exception) {
        error("解析活动目录缓存失败，已回源 MySQL", exception);
      }
    }

    misses.increment();
    List<ActivityView> result = databaseLoader.get();
    try {
      redis.opsForValue().set(key, json.writeValueAsString(result), expiresAfter());
    } catch (RuntimeException | JsonProcessingException exception) {
      error("写入活动目录缓存失败，已保留 MySQL 查询结果", exception);
    }
    return result;
  }

  @Override
  public void invalidate() {
    try {
      redis.opsForValue().increment(VERSION_KEY);
      invalidations.increment();
    } catch (RuntimeException exception) {
      error("使活动目录缓存失效失败，下一次读取仍会由 TTL 兜底", exception);
    }
  }

  private String currentVersion() {
    String version = redis.opsForValue().get(VERSION_KEY);
    return version == null ? "0" : version;
  }

  private String cacheKey(String category, LocalDate from, LocalDate to, String version) {
    String parameters = String.join("\u001f", value(category), value(from), value(to));
    String encoded = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(parameters.getBytes(StandardCharsets.UTF_8));
    return KEY_PREFIX + version + ":" + encoded;
  }

  private String value(Object value) {
    return value == null ? "" : value.toString();
  }

  private Duration expiresAfter() {
    if (jitter.isZero() || jitter.isNegative()) {
      return ttl;
    }
    return ttl.plusMillis(ThreadLocalRandom.current().nextLong(jitter.toMillis() + 1));
  }

  private Counter counter(MeterRegistry metrics, String name) {
    return Counter.builder(name).register(metrics);
  }

  private void error(String message, Exception exception) {
    errors.increment();
    log.warn("{}: {}", message, exception.toString());
  }
}
