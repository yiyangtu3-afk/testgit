package com.campuslink.eventing;

import com.campuslink.dto.ActivityNotificationDtos.NotificationView;
import com.campuslink.service.ActivityNotificationRealtimePublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Bridges durable notification events back to the existing authenticated chat WebSocket. */
@Component
@Profile("eventing")
public class ActivityNotificationDeliveryKafkaListener {

  private final ActivityNotificationRealtimePublisher realtime;

  public ActivityNotificationDeliveryKafkaListener(ActivityNotificationRealtimePublisher realtime) {
    this.realtime = realtime;
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-notification-delivery-topic:campuslink.activity.notification.delivery.v1}",
      groupId = "campuslink-activity-notification-websocket-v1")
  void deliver(ActivityNotificationDeliveryMessage message) {
    realtime.publishActivityNotification(message.recipientId(), new NotificationView(
        message.notificationId(), message.activityId(), message.type(), message.title(), message.body(),
        false, message.createdAt()));
  }
}
