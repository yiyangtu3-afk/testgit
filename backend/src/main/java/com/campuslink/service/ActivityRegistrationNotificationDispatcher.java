package com.campuslink.service;

import com.campuslink.entity.ActivityEntity;

public interface ActivityRegistrationNotificationDispatcher {

  void recordRegistrationResult(
      ActivityEntity activity, String attendeeId, String status, int queuePosition);

  void recordPromotion(ActivityEntity activity, String attendeeId);
}
