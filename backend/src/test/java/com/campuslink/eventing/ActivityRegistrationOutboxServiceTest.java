package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityRegistrationOutboxServiceTest {

  @Test void writesVersionedRegistrationMessageToOutbox() throws Exception {
    var events = new RecordingOutboxEventRepository();
    var service = new ActivityRegistrationOutboxService(events,
        new ObjectMapper().findAndRegisterModules());

    service.enqueue(new ActivityRegistrationEventEntity("event-1", "registration-1", "activity-1",
        "student-1", "student-1", "registered", null, "registered",
        LocalDateTime.of(2026, 7, 25, 12, 0)));

    assertThat(events.created).singleElement().satisfies(event -> {
      assertThat(event.aggregateType()).isEqualTo("activity-registration");
      assertThat(event.aggregateId()).isEqualTo("registration-1");
      assertThat(event.eventType()).isEqualTo("activity.registration.registered.v1");
    });
    ActivityRegistrationMessage message = new ObjectMapper().findAndRegisterModules().readValue(
        events.created.getFirst().payload(), ActivityRegistrationMessage.class);
    assertThat(message).extracting(ActivityRegistrationMessage::eventId,
        ActivityRegistrationMessage::activityId, ActivityRegistrationMessage::toStatus)
        .containsExactly("event-1", "activity-1", "registered");
  }

  private static final class RecordingOutboxEventRepository implements OutboxEventRepository {
    private final List<CreatedEvent> created = new ArrayList<>();

    @Override public void create(String aggregateType, String aggregateId, String eventType, String payload) {
      created.add(new CreatedEvent(aggregateType, aggregateId, eventType, payload));
    }
    @Override public List<OutboxEventEntity> findReady(int limit) { return List.of(); }
    @Override public void markPublished(String eventId) { }
    @Override public void markRetry(String eventId, String message, int retryDelaySeconds) { }
    @Override public OutboxEventEntity findById(String eventId) { return null; }
  }

  private record CreatedEvent(String aggregateType, String aggregateId, String eventType, String payload) {
  }
}
