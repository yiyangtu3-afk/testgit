package com.campuslink.service;

import com.campuslink.entity.ActivityEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!eventing")
public class InProcessActivityRegistrationNotificationDispatcher
    implements ActivityRegistrationNotificationDispatcher {

  private final ActivityNotificationService notifications;

  public InProcessActivityRegistrationNotificationDispatcher(ActivityNotificationService notifications) {
    this.notifications = notifications;
  }

  @Override
  public void recordRegistrationResult(
      ActivityEntity activity, String attendeeId, String status, int queuePosition) {
    notifications.recordRegistrationResult(activity, attendeeId, status, queuePosition);
  }

  @Override
  public void recordPromotion(ActivityEntity activity, String attendeeId) {
    notifications.recordPromotion(activity, attendeeId);
  }
}
