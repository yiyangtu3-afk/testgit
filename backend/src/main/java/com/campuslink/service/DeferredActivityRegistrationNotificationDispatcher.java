package com.campuslink.service;

import com.campuslink.entity.ActivityEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("eventing")
public class DeferredActivityRegistrationNotificationDispatcher
    implements ActivityRegistrationNotificationDispatcher {

  @Override
  public void recordRegistrationResult(
      ActivityEntity activity, String attendeeId, String status, int queuePosition) {
    // Kafka projection creates registration notifications after the Outbox event is consumed.
  }

  @Override
  public void recordPromotion(ActivityEntity activity, String attendeeId) {
    // Kafka projection creates promotion notifications after the Outbox event is consumed.
  }
}
