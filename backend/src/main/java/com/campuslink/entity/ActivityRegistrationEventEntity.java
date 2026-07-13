package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityRegistrationEventEntity(
    String id,
    String registrationId,
    String activityId,
    String attendeeId,
    String actorId,
    String eventType,
    String fromStatus,
    String toStatus,
    LocalDateTime createdAt) {
}
