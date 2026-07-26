package com.campuslink.eventing;

import java.time.LocalDateTime;

/** Event emitted by notification-service after durable notification persistence. */
public record ActivityNotificationDeliveryMessage(
    String recipientId,
    String notificationId,
    String activityId,
    String type,
    String title,
    String body,
    LocalDateTime createdAt) {
}
