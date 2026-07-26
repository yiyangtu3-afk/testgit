package com.campuslink.entity;

import java.time.LocalDateTime;

public record OutboxEventEntity(
    String id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String payload,
    String status,
    int attempts,
    LocalDateTime nextAttemptAt,
    LocalDateTime publishedAt,
    String lastError,
    LocalDateTime createdAt) {
}
