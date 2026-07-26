package com.campuslink.activity.domain;

import java.time.LocalDateTime;

public record ActivityRecord(String id, String title, String description, String category,
    String location, LocalDateTime startsAt, LocalDateTime endsAt, int capacity,
    String organizerId, String status, String reviewDecision, String reviewReason,
    String reviewerId, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
