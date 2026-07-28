package com.campuslink.activity.idempotency;

import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "campuslink.redis.registration-idempotency.enabled", havingValue = "true")
public class RedisRegistrationIdempotency implements RegistrationIdempotency {
  private static final Logger log = LoggerFactory.getLogger(RedisRegistrationIdempotency.class);
  private static final String PREFIX = "campuslink:activities:registration:idempotency:";
  private static final String PROCESSING = "processing:";
  private static final String COMPLETED = "completed:";
  private static final DefaultRedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]); end; "
          + "return 0;",
      Long.class);

  private final StringRedisTemplate redis;
  private final ObjectMapper json;
  private final byte[] signingSecret;
  private final Duration processingTtl;
  private final Duration resultTtl;
  private final Counter claimed;
  private final Counter replayed;
  private final Counter inFlight;
  private final Counter errors;

  public RedisRegistrationIdempotency(
      StringRedisTemplate redis,
      ObjectMapper json,
      MeterRegistry metrics,
      @Value("${campuslink.security.jwt.secret}") String signingSecret,
      @Value("${campuslink.redis.registration-idempotency.processing-ttl:30s}") Duration processingTtl,
      @Value("${campuslink.redis.registration-idempotency.result-ttl:24h}") Duration resultTtl) {
    if (processingTtl.isZero() || processingTtl.isNegative() || resultTtl.isZero() || resultTtl.isNegative()) {
      throw new IllegalArgumentException("Redis 报名幂等配置必须为正数");
    }
    this.redis = redis;
    this.json = json;
    this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    this.processingTtl = processingTtl;
    this.resultTtl = resultTtl;
    this.claimed = counter(metrics, "campuslink.redis.registration_idempotency.claimed");
    this.replayed = counter(metrics, "campuslink.redis.registration_idempotency.replayed");
    this.inFlight = counter(metrics, "campuslink.redis.registration_idempotency.in_flight");
    this.errors = counter(metrics, "campuslink.redis.registration_idempotency.error");
  }

  @Override
  public RegistrationView execute(
      String userId, String activityId, String idempotencyKey, Supplier<RegistrationView> action) {
    String key = key(userId, activityId, idempotencyKey);
    String owner = PROCESSING + UUID.randomUUID();
    boolean acquired;
    try {
      acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, processingTtl));
    } catch (RuntimeException exception) {
      return unavailable(action, exception);
    }
    if (acquired) {
      claimed.increment();
      return executeAsOwner(key, owner, action);
    }
    return replay(key, action);
  }

  private RegistrationView executeAsOwner(
      String key, String owner, Supplier<RegistrationView> action) {
    try {
      RegistrationView result = action.get();
      try {
        redis.opsForValue().set(key, COMPLETED + json.writeValueAsString(result), resultTtl);
      } catch (RuntimeException | JsonProcessingException exception) {
        error("保存报名幂等结果失败，已保留数据库结果", exception);
      }
      return result;
    } catch (RuntimeException exception) {
      release(key, owner);
      throw exception;
    }
  }

  private RegistrationView replay(String key, Supplier<RegistrationView> action) {
    String stored;
    try {
      stored = redis.opsForValue().get(key);
    } catch (RuntimeException exception) {
      return unavailable(action, exception);
    }
    if (stored != null && stored.startsWith(COMPLETED)) {
      try {
        replayed.increment();
        return json.readValue(stored.substring(COMPLETED.length()), RegistrationView.class);
      } catch (JsonProcessingException exception) {
        error("读取报名幂等结果失败", exception);
        throw conflict("幂等结果不可用，请使用新的 Idempotency-Key 重试");
      }
    }
    inFlight.increment();
    throw conflict("请求正在处理中，请稍后重试");
  }

  private RegistrationView unavailable(Supplier<RegistrationView> action, RuntimeException exception) {
    error("Redis 报名幂等不可用，已执行原有报名流程", exception);
    return action.get();
  }

  private void release(String key, String owner) {
    try {
      redis.execute(RELEASE_IF_OWNER, List.of(key), owner);
    } catch (RuntimeException exception) {
      error("释放报名幂等处理标记失败，过期后会自动清理", exception);
    }
  }

  private String key(String userId, String activityId, String idempotencyKey) {
    if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._-]{8,128}")) {
      throw new IllegalArgumentException("Idempotency-Key 必须为 8 到 128 位字母、数字、点、下划线或连字符");
    }
    return PREFIX + fingerprint(userId + "\u001f" + activityId + "\u001f" + idempotencyKey);
  }

  private String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("无法生成报名幂等键", exception);
    }
  }

  private Counter counter(MeterRegistry metrics, String name) {
    return Counter.builder(name).register(metrics);
  }

  private void error(String message, Exception exception) {
    errors.increment();
    log.warn("{}: {}", message, exception.toString());
  }

  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
