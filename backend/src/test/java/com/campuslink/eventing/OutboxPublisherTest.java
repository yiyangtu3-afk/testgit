package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

  @Test void marksEventPublishedAfterBrokerAcknowledgesIt() throws Exception {
    var events = new RecordingRepository();
    var received = new ArrayList<ActivityRegistrationMessage>();
    var publisher = new OutboxPublisher(events, received::add,
        new ObjectMapper().findAndRegisterModules(), new EventingProperties("activity.events", 5));

    publisher.publish(List.of(event("outbox-1")));

    assertThat(received).singleElement().extracting(ActivityRegistrationMessage::eventId)
        .isEqualTo("event-1");
    assertThat(events.published).containsExactly("outbox-1");
    assertThat(events.retried).isEmpty();
  }

  @Test void retainsEventForRetryWhenBrokerPublishFails() throws Exception {
    var events = new RecordingRepository();
    var publisher = new OutboxPublisher(events, message -> {
      throw new IllegalStateException("broker unavailable");
    }, new ObjectMapper().findAndRegisterModules(), new EventingProperties("activity.events", 5));

    publisher.publish(List.of(event("outbox-2")));

    assertThat(events.published).isEmpty();
    assertThat(events.retried).containsExactly("outbox-2:broker unavailable:5");
  }

  private OutboxEventEntity event(String id) throws Exception {
    var mapper = new ObjectMapper().findAndRegisterModules();
    String payload = mapper.writeValueAsString(new ActivityRegistrationMessage("event-1",
        "activity.registration.registered.v1", "registration-1", "activity-1", "student-1",
        "student-1", null, "registered", LocalDateTime.of(2026, 7, 25, 12, 0),
        "校园编程赛", 0));
    return new OutboxEventEntity(id, "activity-registration", "registration-1",
        "activity.registration.registered.v1", payload, "pending", 0,
        LocalDateTime.now(), null, null, LocalDateTime.now());
  }

  private static final class RecordingRepository implements OutboxEventRepository {
    private final List<String> published = new ArrayList<>();
    private final List<String> retried = new ArrayList<>();

    @Override public void create(String aggregateType, String aggregateId, String eventType, String payload) { }
    @Override public List<OutboxEventEntity> findReady(int limit) { return List.of(); }
    @Override public void markPublished(String eventId) { published.add(eventId); }
    @Override public void markRetry(String eventId, String message, int retryDelaySeconds) {
      retried.add(eventId + ":" + message + ":" + retryDelaySeconds);
    }
    @Override public OutboxEventEntity findById(String eventId) { return null; }
  }
}
