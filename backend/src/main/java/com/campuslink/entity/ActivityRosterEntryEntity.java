package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityRosterEntryEntity(
    String registrationId,
    String activityId,
    String attendeeId,
    String attendeeName,
    String status,
    LocalDateTime registeredAt,
    LocalDateTime waitlistedAt,
    LocalDateTime checkedInAt) {
}
