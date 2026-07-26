package com.campuslink.eventing;

import java.time.LocalDateTime;

public record ActivityRegistrationMessage(
    String eventId,
    String eventType,
    String registrationId,
    String activityId,
    String attendeeId,
    String actorId,
    String fromStatus,
    String toStatus,
    LocalDateTime occurredAt) {
}
