package com.campuslink.activity.eventing;

import com.campuslink.activity.mapper.OutboxEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ActivityReviewEventPublisher {
  private final OutboxEventMapper outbox;
  private final ObjectMapper json;
  public ActivityReviewEventPublisher(OutboxEventMapper outbox, ObjectMapper json) {
    this.outbox = outbox;
    this.json = json;
  }
  public void publish(ActivityReviewEvent event) {
    String decision = event.activity().reviewDecision();
    String type = "approved".equals(decision) ? "activity.review.approved.v1" : "activity.review.rejected.v1";
    ActivityReviewMessage message = new ActivityReviewMessage(
        UUID.randomUUID().toString().replace("-", ""), type, null, event.activity().id(),
        event.activity().organizerId(), event.reviewerId(), "pending",
        "rejected".equals(decision) ? event.activity().reviewReason() : decision,
        event.occurredAt(), event.activity().title(), null);
    outbox.insert(message.eventId(), "activity-review", message.activityId(), message.eventType(), serialize(message));
  }
  private String serialize(ActivityReviewMessage message) {
    try {
      return json.writeValueAsString(message);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("无法序列化活动审核领域事件", exception);
    }
  }
}
