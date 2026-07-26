package com.campuslink.eventing;

import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ActivityRegistrationOutboxService implements ActivityRegistrationEventOutbox {

  private final OutboxEventRepository events;
  private final ObjectMapper objectMapper;

  public ActivityRegistrationOutboxService(OutboxEventRepository events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @Override
  public void enqueue(ActivityRegistrationEventEntity event, ActivityRegistrationEventContext context) {
    ActivityRegistrationMessage message = new ActivityRegistrationMessage(event.id(),
        eventType(event.eventType()), event.registrationId(), event.activityId(), event.attendeeId(),
        event.actorId(), event.fromStatus(), event.toStatus(), event.createdAt(),
        context.activityTitle(), context.queuePosition());
    events.create("activity-registration", event.registrationId(), message.eventType(), json(message));
  }

  private String eventType(String eventType) {
    return "activity.registration." + eventType + ".v1";
  }

  private String json(ActivityRegistrationMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("无法序列化活动报名领域事件", error);
    }
  }
}
