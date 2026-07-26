package com.campuslink.activity.domain;

import java.time.LocalDateTime;

public record RegistrationRecord(String id, String activityId, String attendeeId, String status,
    LocalDateTime registeredAt, LocalDateTime waitlistedAt, LocalDateTime checkedInAt,
    LocalDateTime cancelledAt, LocalDateTime createdAt) {}
