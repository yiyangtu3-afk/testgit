package com.campuslink.activity.idempotency;

import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import java.util.function.Supplier;

public interface RegistrationIdempotency {
  RegistrationView execute(
      String userId, String activityId, String idempotencyKey, Supplier<RegistrationView> action);
}
