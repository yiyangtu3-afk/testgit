package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityEntity(
    String id,
    String title,
    String description,
    String category,
    String location,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    int capacity,
    String organizerId,
    String organizerName,
    String status,
    String reviewDecision,
    String reviewReason,
    String reviewerId,
    String reviewerName,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt) {
}
