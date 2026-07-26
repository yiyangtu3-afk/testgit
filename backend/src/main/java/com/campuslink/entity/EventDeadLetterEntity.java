package com.campuslink.entity;

import java.time.LocalDateTime;

public record EventDeadLetterEntity(
    String id,
    String consumerName,
    String eventId,
    String eventType,
    String eventKey,
    String originalTopic,
    String payload,
    String failureMessage,
    String status,
    int deliveryCount,
    LocalDateTime deadLetteredAt,
    LocalDateTime replayedAt) {
}
