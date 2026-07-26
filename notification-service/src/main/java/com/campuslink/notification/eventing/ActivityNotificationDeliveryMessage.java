package com.campuslink.notification.eventing;

import java.time.LocalDateTime;

public record ActivityNotificationDeliveryMessage(String recipientId, String notificationId,
    String activityId, String type, String title, String body, LocalDateTime createdAt) {
}
