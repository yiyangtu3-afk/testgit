package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityRegistrationEntity(
    String id,
    String activityId,
    String attendeeId,
    String status,
    LocalDateTime registeredAt,
    LocalDateTime waitlistedAt,
    LocalDateTime cancelledAt,
    LocalDateTime createdAt) {
}
