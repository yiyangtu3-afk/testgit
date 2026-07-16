package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.NotificationView;

public interface SocialNotificationRealtimePublisher {

  void publishSocialNotification(String recipientId, NotificationView notification);
}
