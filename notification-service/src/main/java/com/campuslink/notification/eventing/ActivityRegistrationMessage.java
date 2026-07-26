package com.campuslink.notification.eventing;

import java.time.LocalDateTime;

/** Versioned activity contract shared through Kafka, deliberately free of activity-table lookups. */
public record ActivityRegistrationMessage(String eventId, String eventType, String registrationId,
    String activityId, String attendeeId, String actorId, String fromStatus, String toStatus,
    LocalDateTime occurredAt, String activityTitle, Integer queuePosition) {
}
