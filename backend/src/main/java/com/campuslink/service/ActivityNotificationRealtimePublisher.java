package com.campuslink.service;

import com.campuslink.dto.ActivityNotificationDtos.NotificationView;

public interface ActivityNotificationRealtimePublisher {

  void publishActivityNotification(String recipientId, NotificationView notification);
}
