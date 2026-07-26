package com.campuslink.notification.domain;

import java.time.LocalDateTime;

public record ActivityNotification(String id, String recipientId, String activityId, String type,
    String title, String body, LocalDateTime readAt, LocalDateTime createdAt) {
}
