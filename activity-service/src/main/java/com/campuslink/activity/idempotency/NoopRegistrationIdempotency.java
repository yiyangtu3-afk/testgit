package com.campuslink.activity.idempotency;

import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "campuslink.redis.registration-idempotency.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoopRegistrationIdempotency implements RegistrationIdempotency {
  @Override
  public RegistrationView execute(
      String userId, String activityId, String idempotencyKey, Supplier<RegistrationView> action) {
    return action.get();
  }
}
