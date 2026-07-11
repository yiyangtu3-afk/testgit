package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityReviewEntity(
    String id,
    String activityId,
    String actorId,
    String decision,
    String reason,
    LocalDateTime createdAt) {
}
