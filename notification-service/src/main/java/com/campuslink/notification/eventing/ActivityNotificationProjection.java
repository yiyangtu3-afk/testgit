package com.campuslink.notification.eventing;

import com.campuslink.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ActivityNotificationProjection {
  public static final String CONSUMER_NAME = "campuslink-activity-notification-v1";
  private final NotificationService notifications;

  public ActivityNotificationProjection(NotificationService notifications) {
    this.notifications = notifications;
  }

  @KafkaListener(topics = "${campuslink.eventing.activity-topic}", groupId = CONSUMER_NAME,
      containerFactory = "notificationKafkaListenerContainerFactory")
  void project(ActivityRegistrationMessage event) {
    notifications.project(event);
  }
}
