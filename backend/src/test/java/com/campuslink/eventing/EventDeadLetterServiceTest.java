package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.entity.EventDeadLetterEntity;
import com.campuslink.repository.EventDeadLetterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventDeadLetterServiceTest {

  @Test void recordsDuplicateDeadLettersOnceAndReplaysOnlyTheOpenRecord() {
    var events = new RecordingDeadLetterRepository();
    var published = new ArrayList<String>();
    var service = new EventDeadLetterService(events, new ObjectMapper().findAndRegisterModules(),
        (topic, key, event) -> published.add(topic + ":" + key + ":" + event.eventId()));
    var message = message("event-1");

    service.record(message, "activity-1", "campuslink.activity.events.v1", "database unavailable");
    service.record(message, "activity-1", "campuslink.activity.events.v1", "database unavailable");
    String id = events.findDeadLetters(10).getFirst().id();
    service.replay(id);

    assertThat(events.findDeadLetters(10)).isEmpty();
    assertThat(published).containsExactly("campuslink.activity.events.v1:activity-1:event-1");
    assertThatThrownBy(() -> service.replay(id))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("消费者死信事件已重放");
  }

  private ActivityRegistrationMessage message(String eventId) {
    return new ActivityRegistrationMessage(eventId, "activity.registration.registered.v1", "registration-1",
        "activity-1", "student-1", "student-1", null, "registered",
        LocalDateTime.of(2026, 7, 25, 12, 0), "校园编程赛", 0);
  }

  static final class RecordingDeadLetterRepository implements EventDeadLetterRepository {
    private final Map<String, EventDeadLetterEntity> values = new LinkedHashMap<>();

    @Override public void record(String consumerName, String eventId, String eventType, String eventKey,
        String originalTopic, String payload, String failureMessage) {
      EventDeadLetterEntity current = values.values().stream()
          .filter(item -> item.consumerName().equals(consumerName) && item.eventId().equals(eventId))
          .findFirst().orElse(null);
      String id = current == null ? "dead-" + (values.size() + 1) : current.id();
      values.put(id, new EventDeadLetterEntity(id, consumerName, eventId, eventType, eventKey, originalTopic,
          payload, failureMessage, "dead_letter", current == null ? 1 : current.deliveryCount() + 1,
          LocalDateTime.of(2026, 7, 25, 13, 0), null));
    }
    @Override public List<EventDeadLetterEntity> findDeadLetters(int limit) {
      return values.values().stream().filter(item -> "dead_letter".equals(item.status())).limit(limit).toList();
    }
    @Override public Optional<EventDeadLetterEntity> findById(String id) { return Optional.ofNullable(values.get(id)); }
    @Override public boolean markReplayed(String id) {
      EventDeadLetterEntity event = values.get(id);
      if (event == null || !"dead_letter".equals(event.status())) return false;
      values.put(id, new EventDeadLetterEntity(event.id(), event.consumerName(), event.eventId(), event.eventType(),
          event.eventKey(), event.originalTopic(), event.payload(), event.failureMessage(), "replayed",
          event.deliveryCount(), event.deadLetteredAt(), LocalDateTime.of(2026, 7, 25, 14, 0)));
      return true;
    }
    @Override public int countDeadLetters() { return (int) values.values().stream()
        .filter(item -> "dead_letter".equals(item.status())).count(); }
  }
}
