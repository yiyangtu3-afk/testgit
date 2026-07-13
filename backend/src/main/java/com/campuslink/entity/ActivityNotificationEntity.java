package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityNotificationEntity(
    String id,
    String recipientId,
    String activityId,
    String type,
    String title,
    String body,
    LocalDateTime readAt,
    LocalDateTime createdAt) {
}
