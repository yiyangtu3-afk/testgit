package com.campuslink.activity.eventing;

import java.time.LocalDateTime;

/** JSON-compatible versioned event that the notification service can consume without a table lookup. */
public record ActivityReviewMessage(String eventId, String eventType, String registrationId,
    String activityId, String attendeeId, String actorId, String fromStatus, String toStatus,
    LocalDateTime occurredAt, String activityTitle, Integer queuePosition) {}
