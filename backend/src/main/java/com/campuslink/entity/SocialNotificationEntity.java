package com.campuslink.entity;

import java.time.LocalDateTime;

public record SocialNotificationEntity(
    String id,
    String recipientId,
    String actorId,
    String targetId,
    String type,
    String title,
    String body,
    LocalDateTime readAt,
    LocalDateTime createdAt) {
}
